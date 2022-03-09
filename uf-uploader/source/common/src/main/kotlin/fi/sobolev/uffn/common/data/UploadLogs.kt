package fi.sobolev.uffn.common.data

import java.time.Instant
import org.ktorm.entity.*
import org.ktorm.schema.*


interface UploadLog : Entity<UploadLog> {
    val id: Int
    var upload: Upload
    var time: Instant
    var level: LogLevel
    var message: String
}

object UploadLogs : Table<UploadLog>("upload_logs") {
    val id = int("id").primaryKey().bindTo { it.id }
    val uploadGuid = uuid("upload_guid").bindTo { it.upload.guid }
    val time = timestamp("time").bindTo { it.time }
    val level = enum<LogLevel>("level").bindTo { it.level }
    val message = varchar("message").bindTo { it.message }
}
