package fi.sobolev.uffn.common.server

import fi.sobolev.uffn.common.fetching.StoryUrl
import io.javalin.websocket.WsContext
import java.util.*
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging


private val logger = KotlinLogging.logger {}

// uuid regex from https://stackoverflow.com/a/20044013
private val uuidPattern = """^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$""".toRegex()


fun handleAuthLogin(ctrl: AuthController, ctx: WsContext, req: AuthLoginRequest) {
    val remoteHost = ctx.session.remoteAddress.hostString
    val (user, active) = ctrl.uploadSessionService.findKey(req.key)

    if (user == null) {
        logger.error { "failed to authenticate request from $remoteHost because key was invalid" }
        ctx.sendPayload(AuthLoginFailedResponse("invalid session key"))
        return
    }

    if (!active) {
        logger.error { "failed to authenticate request from $remoteHost because session expired" }
        ctx.sendPayload(AuthLoginFailedResponse("session expired"))
        return
    }

    logger.info { "authenticated request from $remoteHost" }
    runBlocking { ctrl.sessions.addContext(ctx, user) }

    val response = AuthLoginSucceededResponse (
        uploads = ctrl.uploadService
            .findAllFor(owner = user)
            .map { UploadEntry(entity = it) }
            .toMutableList()
    )

    ctx.sendPayload(response)
}

fun handleUploadCreate(ctrl: UploadController, ctx: WsContext, req: UploadCreateRequest) {
    val user = runBlocking { ctrl.sessions.getUser(ctx) }
    if (user == null) {
        ctx.sendPayload(ErrorResponse("failed to map session to user"))
        return
    }

    val response = UploadCreatedResponse()
    req.urls.forEach { str ->
        val storyInfo = StoryUrl.parse(str)
        if (storyInfo == null) {
            response.addFailed(url = str, reason = "input url not supported")
            return@forEach
        }

        val (archive, identifier) = storyInfo
        val (newUpload, error) = ctrl.uploads.createFor(user, archive, identifier)

        if (newUpload == null) {
            response.addFailed(url = str, reason = error)
            return@forEach
        }

        response.addCreated(newUpload)
    }

    // debug
    response.created.forEach { entry ->
        logger.warn { "created upload with uuid = ${entry.guid}" }
    }
    // end debug

    ctrl.sendTo(user, payload = response)
}

fun handleUploadRemove(ctrl: UploadController, ctx: WsContext, req: UploadRemoveRequest) {
    val user = runBlocking { ctrl.sessions.getUser(ctx) }
    if (user == null) {
        ctx.sendPayload(ErrorResponse("failed to map session to user"))
        return
    }

    val response = UploadRemovedResponse()
    req.guids.forEach { str ->
        if (uuidPattern.matchEntire(str) == null) {
            response.addFailed(str, "not a valid guid")
            return@forEach
        }
        val guid = UUID.fromString(str)
        val (deleted, why) = ctrl.uploads.deleteFor(owner = user, guid)
        if (!deleted) {
            response.addFailed(str, why)
            return@forEach
        }
        response.addRemoved(str)
    }

    ctrl.sendTo(user, payload = response)
}
