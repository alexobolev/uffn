package fi.sobolev.uffn.data

import org.ktorm.entity.*
import org.ktorm.schema.*


interface Author : Entity<Author> {
    val id: Int
    var version: Version
    var name: String
}

object Authors : Table<Author>("story_version_authors") {
    val id = int("id").primaryKey().bindTo { it.id }
    val versionId = int("story_version_id").bindTo { it.version.id }
    val name = varchar("name").bindTo { it.name }
}
