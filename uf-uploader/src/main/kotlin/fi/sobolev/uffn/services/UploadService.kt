package fi.sobolev.uffn.services

import fi.sobolev.uffn.data.*
import java.util.UUID
import org.ktorm.database.Database
import org.ktorm.dsl.and
import org.ktorm.dsl.eq
import org.ktorm.dsl.less
import org.ktorm.entity.*


interface IUploadService {
    fun findOne(guid: UUID): Upload
    fun exists(guid: UUID): Boolean
    fun inFlight(owner: User, archive: Archive, identifier: String): Boolean
    fun create(owner: User, archive: Archive, identifier: String): Upload
}


class LocalUploadService (
    private val db: Database
) : IUploadService {

    override fun findOne(guid: UUID): Upload {
        return db.sequenceOf(Uploads).find { it.guid eq guid }!!
    }

    override fun exists(guid: UUID): Boolean {
        return db.sequenceOf(Uploads).any { it.guid eq guid }
    }

    override fun inFlight(owner: User, archive: Archive, identifier: String): Boolean {
        return db.sequenceOf(Uploads).any {
            ((it.archive eq archive) and (it.identifier eq identifier)) and
                    ((it.ownerId eq owner.id) and (it.status less UploadStatus.COMPLETED))
        }
    }

    override fun create(owner: User, archive: Archive, identifier: String): Upload {
        return Entity.create<Upload>().also {
            it.guid = UUID.randomUUID()
            it.owner = owner
            it.archive = archive
            it.identifier = identifier
            it.status = UploadStatus.PENDING
        }.also {
            db.sequenceOf(Uploads).add(it)
        }
    }
}
