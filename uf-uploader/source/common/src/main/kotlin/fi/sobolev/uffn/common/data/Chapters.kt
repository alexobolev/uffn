package fi.sobolev.uffn.common.data

import org.ktorm.entity.*
import org.ktorm.schema.*


interface Chapter : Entity<Chapter> {
    val id: Int
    var version: Version
    var sequenceNum: Int
    var title: String?
    var summary: String?
    var notesPre: String?
    var notesPost: String?
    var contents: String
}

object Chapters : Table<Chapter>("story_version_chapters") {
    val id = int("id").primaryKey().bindTo { it.id }
    val versionId = int("story_version_id").bindTo { it.version.id }
    val sequenceNum = int("sequence_num").bindTo { it.sequenceNum }
    val title = varchar("title").bindTo { it.title }
    val summary = text("summary").bindTo { it.summary }
    val notesPre = text("notes_pre").bindTo { it.notesPre }
    val notesPost = text("notes_post").bindTo { it.notesPost }
    val contents = text("contents").bindTo { it.contents }
}
