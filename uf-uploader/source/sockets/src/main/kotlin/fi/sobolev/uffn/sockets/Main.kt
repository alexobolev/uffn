package fi.sobolev.uffn.sockets

import fi.sobolev.uffn.common.*
import fi.sobolev.uffn.common.server.*
import fi.sobolev.uffn.common.services.*

import io.javalin.Javalin
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import mu.KotlinLogging


private lateinit var sessionRegistry: SessionRegistry
private lateinit var controllers: Map<String, BaseController>

private val shouldShutdown =  AtomicBoolean(false)

private val logger = KotlinLogging.logger {}
private val requestPattern = """(\w+)\.(.+)""".toRegex()


fun main(args: Array<String>) {
    // fixes a null pointer exception in json serialization
    System.setProperty("kotlinx.serialization.json.pool.size", (1024 * 1024).toString())

    gAppConfig = Config (
        database = Config.PostgresConfig (
            host = "localhost",
            port = 25001,
            user = "argon",
            pass = "qwerty",
            name = "dev_uffn"
        ),
        redis = Config.RedisConfig (
            host = "localhost",
            port = 25000,
            interval = 5,
            maxTotal = 16
        ),
        websockets = Config.WsConfig (
            port = 7070
        )
    )

    gDbConn = makeDbConnection(gAppConfig.database)
    gRedisConn = makeRedisConnection(gAppConfig.redis)
    sessionRegistry = DefaultSessionRegistry()
    controllers = makeControllers()

    // update notification queue listener has a new thread
    startRedisListener(gAppConfig.redis)

    // websocket server blocks the main thread
    startJavalinApp(gAppConfig.websockets)
}

fun makeControllers(): Map<String, BaseController> {
    val uploadService = LocalUploadService(gDbConn, gRedisConn)
    val sessionService = LocalSessionService(gDbConn)

    // handles all login-logout logic
    val authController = AuthController (
        sessions = sessionRegistry,
        uploadSessionService = sessionService,
        uploadService = uploadService
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
    }

    return mapOf (
        "auth" to authController,
        "upload" to uploadController
    )
}


fun startRedisListener(config: Config.RedisConfig) {
    val pollThread = Thread {
        gRedisConn.use { pool ->
            val uploadService = LocalUploadService(gDbConn, gRedisConn)

            val kInputQueue = "uffn-update"
            val kBlockInterval = config.interval.toDouble()

            logger.info { "starting polling the update notification queue (UNQ)" }

            pool.resource.use { conn ->
                while (!shouldShutdown.get()) {
                    val item = conn.blpop(kBlockInterval, kInputQueue) ?: continue

                    val queue = item.key
                    val task = item.element

                    logger.info { "UNQ loop - retrieved $task from $queue" }

                    val uuid = task.trim().tryParseUuid()
                    if (uuid == null) {
                        logger.warn { "failed to match UNQ task (should be a UUID, was $task)" }
                        continue
                    }

                    val upload = uploadService.findOne(uuid)
                    if (upload == null) {
                        logger.warn { "UNQ task contained an UUID which didn't match any upload: $uuid" }
                        continue
                    }

                    val notification = UploadInfoResponse().apply {
                        uploads.add(UploadEntry(entity = upload))
                    }

                    runBlocking {
                        sessionRegistry.forUser(upload.owner) {
                            it.sendPayload(notification)
                        }
                    }
                }
            }

            logger.info { "finished polling UNQ" }
        }
    }

    // register shutdown logic
    Runtime.getRuntime().addShutdownHook(object : Thread() {
        override fun run() {
            try {
                logger.info { "UNQ termination flag set, should shutdown in about ${config.interval} seconds" }
                shouldShutdown.set(true)
                pollThread.join()
            } catch (ex: InterruptedException) {
                logger.error(ex) { "exception thrown during shutdown - $ex" }
            }
        }
    })

    pollThread.start()
}

fun startJavalinApp(config: Config.WsConfig) {
    val app = Javalin.create().start(config.port)

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
}
