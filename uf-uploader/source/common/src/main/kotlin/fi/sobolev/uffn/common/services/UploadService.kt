package fi.sobolev.uffn.common.services

import fi.sobolev.uffn.common.data.*
import java.util.UUID
import org.ktorm.database.Database
import org.ktorm.dsl.and
import org.ktorm.dsl.eq
import org.ktorm.dsl.less
import org.ktorm.entity.*
import redis.clients.jedis.JedisPool


interface IUploadService {
    fun findOne(guid: UUID): Upload?
    fun findOneFor(owner: User, guid: UUID): Upload?
    fun getLogs(guid: UUID): List<UploadLog>
    fun existsFor(owner: User, guid: UUID): Boolean
    fun createFor(owner: User, archive: Archive, identifier: String): Pair<Upload?, String>
    fun deleteFor(owner: User, guid: UUID): Pair<Boolean, String>
}


class LocalUploadService (
    private val db: Database,
    private val redis: JedisPool
) : IUploadService {
    override fun findOne(guid: UUID): Upload? {
        return db.sequenceOf(Uploads).firstOrNull { it.guid eq guid }
    }

    override fun findOneFor(owner: User, guid: UUID): Upload? {
        return db.sequenceOf(Uploads).firstOrNull {
            (it.guid eq guid) and (it.ownerId eq owner.id)
        }
    }

    override fun getLogs(guid: UUID): List<UploadLog> {
        return db.sequenceOf(UploadLogs)
            .filter { it.uploadGuid eq guid }
            .sortedBy { it.time }
            .toList()
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
        }

        val justCreated = uploads.find { it.guid eq upload.guid }
            ?: return Pair(null, "internal entity creation failed")

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
