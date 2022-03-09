package fi.sobolev.uffn.server

import fi.sobolev.uffn.data.User
import fi.sobolev.uffn.services.IUploadService
import fi.sobolev.uffn.services.IUserService
import io.javalin.websocket.WsContext
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement


typealias HandlerCallback = suspend (WsContext, JsonElement) -> Unit

inline fun <reified T : ServerPayload> WsContext.sendPayload(payload: T) {
    val wrapper = ServerMessage (
        response = payload.code,
        payload = Json.encodeToJsonElement(payload)
    )
    this.send(Json.encodeToString(wrapper))
}


abstract class BaseController (
    val sessions: SessionRegistry
) {
    val handlerMap = mutableMapOf<String, HandlerCallback>()

    abstract fun before(ctx: WsContext, payload: JsonElement): Boolean

    inline fun <reified T: ServerPayload> sendTo(user: User, payload: T): Unit = runBlocking {
        sessions.forUser(user) {
            it.sendPayload(payload)
        }
    }

    inline fun <reified C : BaseController, reified T : ClientPayload> register (
        request: String,
        crossinline callback: suspend (C, WsContext, T) -> Unit
    ) {
        handlerMap[request.uppercase()] = { ctx, payload ->
            if (before(ctx, payload)) {
                val decodedPayload = Json.decodeFromJsonElement<T>(payload)
                callback(this as C, ctx, decodedPayload)
            }
        }
    }
}

class AuthController (
    sessions: SessionRegistry,
    val users: IUserService
) : BaseController(sessions) {

    override fun before(ctx: WsContext, payload: JsonElement): Boolean {
        return true
    }
}

class UploadController (
    sessions: SessionRegistry,
    val uploads: IUploadService,
) : BaseController(sessions) {

    override fun before(ctx: WsContext, payload: JsonElement): Boolean {
        return runBlocking {
            if (!sessions.hasContext(ctx)) {
                ctx.sendPayload(ErrorResponse("must authenticate first"))
                return@runBlocking false
            }
            return@runBlocking true
        }
    }

}
