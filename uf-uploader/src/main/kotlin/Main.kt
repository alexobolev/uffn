import fi.sobolev.uffn.Config
import fi.sobolev.uffn.gDbConn
import fi.sobolev.uffn.fetching.StoryUrl
import fi.sobolev.uffn.server.*
import fi.sobolev.uffn.services.*
import io.javalin.Javalin
import io.javalin.websocket.WsContext
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import org.apache.commons.dbcp2.BasicDataSource
import org.ktorm.database.Database
import org.ktorm.support.postgresql.PostgreSqlDialect
import java.util.*


private lateinit var sessionRegistry: SessionRegistry
private lateinit var controllers: Map<String, BaseController>
private lateinit var server: Javalin

private val logger = KotlinLogging.logger {}
private val requestPattern = """(\w+)\.(.+)""".toRegex()
private val uuidPattern = """^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$""".toRegex()

// uuid regex from https://stackoverflow.com/a/20044013


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


fun main(args: Array<String>) {
    // fixes a null pointer exception in json serialization
    System.setProperty("kotlinx.serialization.json.pool.size", (1024 * 1024).toString())

    gDbConn = makeDbConnection(Config.PostgresConfig (
        host = "localhost",
        port = 25001,
        user = "argon",
        pass = "qwerty",
        name = "dev_uffn"
    ))

    sessionRegistry = DefaultSessionRegistry()
    controllers = makeControllers()
    server = makeJavalinApp()
}

fun makeDbConnection(config: Config.PostgresConfig): Database {
    (object : BasicDataSource() {
        init {
            driverClassName = "org.postgresql.Driver"
            url = "jdbc:postgresql://${config.host}:${config.port}/${config.name}"
            username = config.user
            password = config.pass
        }
    }).let { dataSource ->
        return Database.connect(dataSource, PostgreSqlDialect())
    }
}

fun makeControllers(): Map<String, BaseController> {
    val uploadService = LocalUploadService(gDbConn)
    val userService = LocalUserService(gDbConn)

    // handles all login-logout logic
    val authController = AuthController (
        sessions = sessionRegistry,
        users = userService
    ).apply {
        register("login", ::handleAuthLogin)
    }

    // handles upload entity creation
    val uploadController = UploadController (
        sessions = sessionRegistry,
        uploads = uploadService
    ).apply {
        register("create", ::handleUploadCreate)
        register("remove", ::handleUploadRemove)
        register("get-logs", ::handleUploadGetLogs)
    }

    return mapOf (
        "auth" to authController,
        "upload" to uploadController
    )
}

fun makeJavalinApp(): Javalin {
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

            val controller = controllers[codeMatch.groupValues[1]]
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
                sessionRegistry.removeContext(ctx)
            }
        }
        ws.onError { ctx ->
            logger.error { "error on connection with ${ctx.session.remoteAddress}: ${ctx.error()}" }
            runBlocking {
                sessionRegistry.removeContext(ctx)
            }
        }
    }
    return app
}
