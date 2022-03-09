package fi.sobolev.uffn.data


import org.ktorm.entity.*
import org.ktorm.schema.*


interface Story : Entity<Story> {
    val id: Int
    var originArchive: Archive
    var originIdent: String
    var isPublic: Boolean
    var owner: User?
    var summary: String?
    var ratingOverride: Rating?
}

object Stories : Table<Story>("stories") {
    val id = int("id").primaryKey().bindTo { it.id }
    val originArchive = enum<Archive>("origin_archive").bindTo { it.originArchive }
    val originIdent = varchar("origin_identifier").bindTo { it.originIdent }
    val isPublic = boolean("is_public").bindTo { it.isPublic }
    val ownerId = int("owner_id").bindTo { it.owner?.id }
    val summary = varchar("owner_summary").bindTo { it.summary }
    val rating = enum<Rating>("owner_rating").bindTo { it.ratingOverride }
}
