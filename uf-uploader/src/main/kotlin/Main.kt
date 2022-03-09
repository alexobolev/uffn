import fi.sobolev.uffn.Config
import fi.sobolev.uffn.DbConn
import fi.sobolev.uffn.data.*
import fi.sobolev.uffn.fetching.StoryUrl
import fi.sobolev.uffn.makeDbConnection
import fi.sobolev.uffn.server.*
import fi.sobolev.uffn.services.LocalUploadService
import io.javalin.Javalin
import io.javalin.websocket.WsContext
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import org.ktorm.dsl.*
import org.ktorm.entity.*


private val logger = KotlinLogging.logger {}
private val requestPattern = """(\w+)\.(.+)""".toRegex()


fun handleAuthLogin(ctrl: BaseController, ctx: WsContext, req: AuthLoginRequest) {
    if (req.user == 1 && req.key == "TESTTESTTEST") {
        val user = DbConn.sequenceOf(Users).firstOrNull { it.id eq req.user }
        if (user == null) {
            ctx.sendPayload(ErrorResponse("failed to authenticate test/dev connection"))
            return
        }
        logger.info { "authenticated user ${user.login} (${user.email}) with test credentials" }
        runBlocking { ctrl.sessionRegistry.addContext(ctx, user) }
        ctx.sendPayload(AuthLoginSucceededResponse())
        return
    }
    ctx.sendPayload(AuthLoginFailedResponse("test credentials not accepted"))
}

fun handleUploadCreate(ctrl: UploadController, ctx: WsContext, req: UploadCreateRequest) {
    val user = runBlocking { ctrl.sessionRegistry.getUser(ctx) }
    if (user == null) {
        ctx.sendPayload(ErrorResponse("failed to map session to user"))
        return
    }

    val response = UploadCreatedResponse()

    // Iterate over the requested URLs, sorting them into
    // either the 'good' list (valid URLs and state)
    // or the 'bad' list (everything else).
    req.urls.forEach { str ->
        val storyInfo = StoryUrl.parse(str)
        if (storyInfo == null) {
            response.addUnrecognized(url = str)
            return@forEach
        }

        // Archive is the target archive (must be supported),
        // Identifier is the in-archive story ID.
        val (archive, identifier) = storyInfo

        DbConn.useTransaction { _ ->
            // Check if there's already an in-flight upload for this story
            // and this user (completed and cancelled ones don't count).
            if (ctrl.uploads.inFlight(user, archive, identifier)) {
                response.addProcessing(url = str)
                return@forEach
            }

            // Insert a new Upload entity into the database.
            val upload = ctrl.uploads.create(user, archive, identifier)
            val justCreated = ctrl.uploads.findOne(guid = upload.guid)

            response.addCreated(justCreated)
        }
    }

    runBlocking {
        ctrl.sessionRegistry.forUser(user) { it.sendPayload(response) }
    }
}

fun handleUploadRemove(ctrl: UploadController, ctx: WsContext, req: UploadRemoveRequest) {
    val user = runBlocking { ctrl.sessionRegistry.getUser(ctx) }
    if (user == null) {
        ctx.sendPayload(ErrorResponse("failed to map session to user"))
        return
    }

    TODO("not implemented")
}

fun handleUploadGetLogs(ctrl: UploadController, ctx: WsContext, req: UploadGetLogsRequest) {
    val user = runBlocking { ctrl.sessionRegistry.getUser(ctx) }
    if (user == null) {
        ctx.sendPayload(ErrorResponse("failed to map session to user"))
        return
    }

    TODO("not implemented")
}


fun main(args: Array<String>) {
    // fixes a null pointer exception in json serialization
    System.setProperty("kotlinx.serialization.json.pool.size", (1024 * 1024).toString())

    DbConn = makeDbConnection(Config.PostgresConfig (
        host = "localhost",
        port = 25001,
        user = "argon",
        pass = "qwerty",
        name = "dev_uffn"
    ))

    val registry = DefaultSessionRegistry()
    val controllers = mapOf (
        "auth" to AuthController(registry).apply {
            register("login", ::handleAuthLogin)
        },
        "upload" to UploadController(registry, LocalUploadService(DbConn)).apply {
            register("create", ::handleUploadCreate)
            register("remove", ::handleUploadRemove)
            register("get-logs", ::handleUploadGetLogs)
        }
    )

    val app = Javalin.create().start(7070)
    app.ws("/upload") { ws ->
        ws.onConnect { ctx ->
            logger.debug { "new connection with ${ctx.session.remoteAddress}" }
        }

        ws.onMessage { ctx ->
            logger.info { "new message on connection with ${ctx.session.remoteAddress}" }
            val message = Json.decodeFromString<ClientMessage>(ctx.message())

            val codeMatch = requestPattern.matchEntire(message.request)
            if (codeMatch == null) {
                ctx.sendPayload(ErrorResponse("failed to parse client payload code"))
                return@onMessage
            }

            val controller = controllers.get(codeMatch.groupValues[1])
            if (controller == null) {
                ctx.sendPayload(ErrorResponse("failed to match client payload controller"))
                return@onMessage
            }

            val handler = controller.handlerMap[codeMatch.groupValues[2].uppercase()]
            if (handler == null) {
                ctx.sendPayload(ErrorResponse("failed to match client payload handler"))
                return@onMessage
            }

            runBlocking {
                handler(ctx, message.payload)
            }
        }

        ws.onClose { ctx ->
            logger.debug { "closing connection with ${ctx.session.remoteAddress}" }
            runBlocking {
                registry.removeContext(ctx)
            }
        }

        ws.onError { ctx ->
            logger.error { "error on connection with ${ctx.session.remoteAddress}: ${ctx.error()}" }
            runBlocking {
                registry.removeContext(ctx)
            }
        }
    }
}
