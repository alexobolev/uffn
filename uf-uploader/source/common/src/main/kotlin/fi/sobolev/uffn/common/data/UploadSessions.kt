package fi.sobolev.uffn.common.data

import java.time.*
import java.util.*
import org.ktorm.entity.*
import org.ktorm.schema.*


interface UploadSession : Entity<UploadSession> {
    var id: Int
    var owner: User
    var authKey: String
    var createdAt: Instant
    var expiresAt: Instant
    var userAgent: String?
    var userAddress: String?
}

object UploadSessions : Table<UploadSession>("upload_sessions") {
    val id = int("id").primaryKey().bindTo { it.id }
    val ownerId = int("owner_id").bindTo { it.owner.id }
    val authKey = varchar("auth_key").bindTo { it.authKey }
    val createdAt = timestamp("created_at").bindTo { it.createdAt }
    val expiresAt = timestamp("expires_at").bindTo { it.expiresAt }
    val userAgent = varchar("user_agent").bindTo { it.userAgent }
    val userAddress = varchar("user_address").bindTo { it.userAddress }
}
