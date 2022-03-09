package fi.sobolev.uffn.common.data

import java.time.Instant
import java.util.*
import org.ktorm.entity.*
import org.ktorm.schema.*


interface Upload : Entity<Upload> {
    var guid: UUID
    var owner: User
    var archive: Archive
    var identifier: String
    var startedAt: Instant
    var title: String?
    var status: UploadStatus
    var errorType: UploadError?
    var errorDesc: String?
    var errorTime: Instant?
}

object Uploads : Table<Upload>("uploads") {
    val guid = uuid("guid").primaryKey().bindTo { it.guid }
    val ownerId = int("owner_id").bindTo { it.owner.id }
    val archive = enum<Archive>("origin_archive").bindTo { it.archive }
    val identifier = varchar("origin_identifier").bindTo { it.identifier }
    val startedAt = timestamp("started_at").bindTo { it.startedAt }
    val title = varchar("title").bindTo { it.title }
    val status = enum<UploadStatus>("status").bindTo { it.status }
    val errorType = enum<UploadError>("error_type").bindTo { it.errorType }
    val errorDesc = varchar("error_desc").bindTo { it.errorDesc }
    val errorTime = timestamp("error_time").bindTo { it.errorTime }
}
