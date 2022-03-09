package fi.sobolev.uffn.common.data

import java.time.Instant
import org.ktorm.entity.*
import org.ktorm.schema.*


interface Version : Entity<Version> {
    val id: Int
    var story: Story
    var archivedAt: Instant
    var hidden: Boolean
    var title: String
    var rating: Rating?
    var summary: String?
    var notesPre: String?
    var notesPost: String?
    var publishedAt: Instant?
    var updatedAt: Instant?
    var isCompleted: Boolean?
}

object Versions : Table<Version>("story_versions") {
    val id = int("id").primaryKey().bindTo { it.id }
    val storyId = int("story_id").bindTo { it.story.id }
    val archivedAt = timestamp("archived_at").bindTo { it.archivedAt }
    val hidden = boolean("is_hidden").bindTo { it.hidden }
    val title = varchar("title").bindTo { it.title }
    val rating = enum<Rating>("rating").bindTo { it.rating }
    val summary = text("summary").bindTo { it.summary }
    val notesPre = text("notes_pre").bindTo { it.notesPre }
    val notesPost = text("notes_post").bindTo { it.notesPost }
    val publishedAt = timestamp("published_at").bindTo { it.publishedAt }
    val updatedAt = timestamp("updated_at").bindTo { it.updatedAt }
    val isCompleted = boolean("is_completed").bindTo { it.isCompleted }
}
