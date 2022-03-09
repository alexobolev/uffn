package fi.sobolev.uffn.server

import fi.sobolev.uffn.fetching.StoryUrl
import io.javalin.websocket.WsContext
import java.util.*
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging


private val logger = KotlinLogging.logger {}

// uuid regex from https://stackoverflow.com/a/20044013
private val uuidPattern = """^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$""".toRegex()


fun handleAuthLogin(ctrl: AuthController, ctx: WsContext, req: AuthLoginRequest) {
    if (req.user == 1 && req.key == "TESTTESTTEST") {
        val user = ctrl.users.findOneOrNull(id = req.user)
        if (user == null) {
            ctx.sendPayload(ErrorResponse("failed to authenticate test/dev connection"))
            return
        }
        logger.info { "authenticated user ${user.login} (${user.email}) with test credentials" }
        runBlocking { ctrl.sessions.addContext(ctx, user) }
        ctx.sendPayload(AuthLoginSucceededResponse())
        return
    }
    ctx.sendPayload(AuthLoginFailedResponse("test credentials not accepted"))
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

fun handleUploadGetLogs(ctrl: UploadController, ctx: WsContext, req: UploadGetLogsRequest) {
    val user = runBlocking { ctrl.sessions.getUser(ctx) }
    if (user == null) {
        ctx.sendPayload(ErrorResponse("failed to map session to user"))
        return
    }

    val upload = ctrl.uploads.findOneFor(owner = user, req.guid)
    if (upload == null) {
        ctx.sendPayload(ErrorResponse("failed to find upload by this guid"))
        return
    }

    val logs = ctrl.uploads.getLogs(upload.guid)

    val response = UploadInfoResponse()
    response.addUpload(upload, logs)

    ctx.sendPayload(response)
}
