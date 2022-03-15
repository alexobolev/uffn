package fi.sobolev.uffn.parser

import fi.sobolev.uffn.common.*
import fi.sobolev.uffn.common.data.*
import fi.sobolev.uffn.common.fetching.StaticBrowser
import fi.sobolev.uffn.common.origins.ao3.AO3Parser
import fi.sobolev.uffn.common.origins.ao3.toCommon
import fi.sobolev.uffn.common.server.*
import fi.sobolev.uffn.common.services.*
import io.javalin.core.util.RouteOverviewUtil.metaInfo
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking

import java.time.*
import java.util.concurrent.atomic.AtomicBoolean
import mu.KotlinLogging
import org.ktorm.dsl.*
import org.ktorm.entity.*

private val shouldShutdown =  AtomicBoolean(false)
private val logger = KotlinLogging.logger {}


fun main(args: Array<String>) {

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

    startRedisListener(gAppConfig.redis)
}

fun startRedisListener(config: Config.RedisConfig) {
    val thisThread = Thread.currentThread()

    // register shutdown logic
    Runtime.getRuntime().addShutdownHook(object : Thread() {
        override fun run() {
            try {
                logger.info { "FQ termination flag set, should shutdown in about ${config.interval} seconds" }
                shouldShutdown.set(true)
                thisThread.join()
            } catch (ex: InterruptedException) {
                logger.error(ex) { "exception thrown during shutdown - $ex" }
            }
        }
    })

    gRedisConn.use { pool ->
        val uploadService = LocalUploadService(gDbConn, gRedisConn)

        val kInputQueue = "uffn-fetch"
        val kNotificationQueue = "uffn-update"
        val kBlockInterval = config.interval.toDouble()

        logger.info { "starting polling the fetch queue (FQ)" }

        pool.resource.use { conn ->
            while (!shouldShutdown.get()) {
                val item = conn.blpop(kBlockInterval, kInputQueue) ?: continue

                val queue = item.key
                val task = item.element

                logger.info { "FQ loop - retrieved $task from $queue" }

                val uuid = task.trim().tryParseUuid()
                if (uuid == null) {
                    logger.warn { "failed to match UNQ task (should be a UUID, was $task)" }
                    continue
                }

                val upload = uploadService.findOne(uuid)
                if (upload == null) {
                    logger.warn { "FQ task contained an UUID which didn't match any upload: $uuid" }
                    continue
                }

                if (upload.archive != Archive.AO3) {
                    logger.error { "parser FQ only supports AO3 stories currently (uuid skipped)" }
                    continue
                }

                if (upload.status != UploadStatus.PENDING) {
                    logger.warn { "FQ task contained an UUID which was already being processed: $uuid" }
                    continue
                }


                upload.status = UploadStatus.FETCHING
                upload.flushChanges()

                conn.rpush(kNotificationQueue, uuid.toString())
                runBlocking { delay(1000) }

                try {
                    logger.info { "starting to parse AO3 story #${upload.identifier}"}

                    val parser = AO3Parser (
                        storyId = upload.identifier.toLong(),
                        browser = StaticBrowser()
                    )
                    val ao3 = parser.parse()

                    gDbConn.useTransaction { _ ->
                        val stories = gDbConn.sequenceOf(Stories)
                        val versions = gDbConn.sequenceOf(Versions)
                        val authors = gDbConn.sequenceOf(Authors)
                        val chapters = gDbConn.sequenceOf(Chapters)

                        val story = stories.firstOrNull {
                            ((it.originArchive eq upload.archive) and
                                    (it.originIdent eq upload.identifier)) and
                                    ((it.ownerId eq upload.owner.id) or
                                            (it.isPublic eq true))
                        } ?: Entity.create<Story>().apply {
                            originArchive = upload.archive
                            originIdent = upload.identifier
                            isPublic = false
                            owner = upload.owner
                        }.also { stories.add(it) }

                        val version = Entity.create<Version>().apply {
                            this.story = story
                            this.archivedAt = Instant.now()
                            this.hidden = true
                            this.title = ao3.title
                            this.rating = ao3.tags.ratings.toCommon()
                            this.summary = ao3.summary
                            this.notesPre = ao3.notesPre
                            this.notesPost = ao3.notesPost
                            this.wordCount = ao3.stats.wordCount.toInt()
                            this.publishedAt = ao3.stats.publishedAt.atStartOfDay().toInstant(ZoneOffset.UTC)
                            this.updatedAt = ao3.stats.updatedAt.atStartOfDay().toInstant(ZoneOffset.UTC)
                            this.isCompleted = ao3.complete
                        }.also { versions.add(it) }

                        ao3.authors.map {
                            Entity.create<Author>().apply {
                                this.version = version
                                this.name = it
                            }
                        }.forEach { authors.add(it) }

                        ao3.chapters.mapIndexed { index, ch ->
                            Entity.create<Chapter>().apply {
                                this.version = version
                                this.sequenceNum = index
                                this.title = ch.title
                                this.summary = ch.summary
                                this.notesPre = ch.notesPre
                                this.notesPost = ch.notesPost
                                this.contents = ch.contents
                            }
                        }.forEach { chapters.add(it) }

                        upload.assocVersion = version
                        upload.title = version.title
                        upload.status = UploadStatus.COMPLETED
                    }

                    logger.info { "finished parsing AO3 story #${upload.identifier}" }

                } catch (ex: Exception) {
                    upload.status = UploadStatus.ERRORED
                    logger.error { "failed to parse AO3 story #${upload.identifier}" }
                    logger.error { "exception: ${ex.message}" }
                    ex.stackTrace.forEach {
                        logger.error { it.toString() }
                    }
                } finally {
                    upload.flushChanges()
                    conn.rpush(kNotificationQueue, uuid.toString())
                }
            }
        }

        logger.info { "finished polling FQ" }
    }
}
