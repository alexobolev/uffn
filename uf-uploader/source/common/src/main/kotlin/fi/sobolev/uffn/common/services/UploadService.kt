package fi.sobolev.uffn.common.services

import fi.sobolev.uffn.common.data.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import java.util.UUID
import org.ktorm.database.Database
import org.ktorm.dsl.and
import org.ktorm.dsl.eq
import org.ktorm.dsl.less
import org.ktorm.entity.*
import redis.clients.jedis.JedisPool


interface IUploadService {
    fun findAllFor(owner: User): List<Upload>
    fun findOne(guid: UUID): Upload?
    fun findOneFor(owner: User, guid: UUID): Upload?
    fun existsFor(owner: User, guid: UUID): Boolean
    fun createFor(owner: User, archive: Archive, identifier: String): Pair<Upload?, String>
    fun deleteFor(owner: User, guid: UUID): Pair<Boolean, String>
}


class LocalUploadService (
    private val db: Database,
    private val redis: JedisPool
) : IUploadService {
    override fun findAllFor(owner: User): List<Upload> {
        return db.sequenceOf(Uploads)
            .filter { it.ownerId eq owner.id }
            .toList()
    }

    override fun findOne(guid: UUID): Upload? {
        return db.sequenceOf(Uploads).firstOrNull { it.guid eq guid }
    }

    override fun findOneFor(owner: User, guid: UUID): Upload? {
        return db.sequenceOf(Uploads).firstOrNull {
            (it.guid eq guid) and (it.ownerId eq owner.id)
        }
    }

    override fun existsFor(owner: User, guid: UUID): Boolean {
        return db.sequenceOf(Uploads).any {
            (it.guid eq guid) and (it.ownerId eq owner.id)
        }
    }

    override fun createFor(owner: User, archive: Archive, identifier: String): Pair<Upload?, String> = db.useTransaction {
        val uploads = db.sequenceOf(Uploads)

        val inFlight = uploads.any {
            ((it.archive eq archive) and (it.identifier eq identifier)) and
                ((it.ownerId eq owner.id) and (it.status less UploadStatus.COMPLETED))
        }
        if (inFlight) {
            return Pair(null, "this story is already being processed")
        }

        val upload = Entity.create<Upload>().also {
            it.guid = UUID.randomUUID()
            it.owner = owner
            it.archive = archive
            it.identifier = identifier
            it.status = UploadStatus.PENDING
        }.also {
            uploads.add(it)
            it.flushChanges()
        }

        val justCreated = uploads.find { it.guid eq upload.guid }
            ?: return Pair(null, "internal entity creation failed")

        runBlocking { delay(1000) }
        redis.resource.use {
            it.rpush("uffn-fetch", upload.guid.toString())
        }

        return Pair(justCreated, "")
    }

    override fun deleteFor(owner: User, guid: UUID): Pair<Boolean, String> = db.useTransaction {
        val uploads = db.sequenceOf(Uploads)
        val upload = uploads.find { (it.guid eq guid ) and (it.ownerId eq owner.id) }
            ?: return Pair(false, "upload with this uuid doesn't exist")

        val status = upload.status
        if (status == UploadStatus.FETCHING) {
            return Pair(false, "upload can't be cancelled at this stage")
        }

        upload.delete()
        return Pair(true, "")
    }
}
