package fi.sobolev.uffn.common.origins.ao3

import java.time.LocalDate


enum class AO3Rating(private val text: String) {
    GeneralAudiences("General Audiences"),
    TeenAndUpAudiences("Teen And Up Audiences"),
    Mature("Mature"),
    Explicit("Explicit"),
    NotRated("Not Rated");

    companion object {
        fun fromString(input: String): AO3Rating {
            for (value in enumValues<AO3Rating>()) {
                if (value.text == input) return value
            }
            throw IllegalArgumentException("can't interpret a string as an ao3 rating")
        }
    }

    override fun toString() = text
}


enum class AO3Warning(private val text: String) {
    CreatorChoseNotToUseArchiveWarnings("Creator Chose Not To Use Archive Warnings"),
    GraphicDepictionsOfViolence("Graphic Depictions Of Violence"),
    MajorCharacterDeath("Major Character Death"),
    NoArchiveWarningsApply("No Archive Warnings Apply"),
    RapeOrNonCon("Rape/Non-Con"),
    Underage("Underage");

    companion object {
        fun fromString(input: String): AO3Warning {
            for (value in enumValues<AO3Warning>()) {
                if (value.text == input) return value
            }
            throw IllegalArgumentException("can't interpret a string as an ao3 warning")
        }
    }

    override fun toString() = text
}


enum class AO3Category(private val text: String) {
    FF("F/F"),
    FM("F/M"),
    Gen("Gen"),
    MM("M/M"),
    Multi("Multi"),
    Other("Other");

    companion object {
        fun fromString(input: String): AO3Category {
            for (value in enumValues<AO3Category>()) {
                if (value.text == input) return value
            }
            throw IllegalArgumentException("can't interpret a string as an ao3 category")
        }
    }

    override fun toString() = text
}


data class AO3Relationship (
    val members: List<String>,
    val romantic: Boolean
) {
    companion object {
        fun fromString(input: String) = AO3Relationship (
            members = input.split("/", "&").map{ it.trim() },
            romantic = input.contains("/")
        )
    }

    override fun toString(): String {
        val separator = if (romantic) "/" else "&"
        return members.joinToString(" $separator ")
    }
}


data class AO3TagsInfo (
    var ratings: List<AO3Rating> = listOf(),
    var warnings: List<AO3Warning> = listOf(),
    var categories: List<AO3Category> = listOf(),
    var fandoms: List<String> = listOf(),
    var relationships: List<AO3Relationship> = listOf(),
    var characters: List<String> = listOf(),
    var freeform: List<String> = listOf()
)


data class AO3StatsInfo (
    var publishedAt: LocalDate = LocalDate.MIN,
    var updatedAt: LocalDate = LocalDate.MIN,
    var wordCount: Long = 0,
    var chaptersDone: Long = 0,
    var chaptersPlanned: Long? = 0,
    var commentCount: Long = 0,
    var kudoCount: Long = 0,
    var bookmarkCount: Long = 0,
    var hitCount: Long = 0
)


data class AO3Chapter (
    var id: Long = 0,
    var index: Long = 0,
    var title: String? = null,
    var summary: String? = null,
    var notesPre: String? = null,
    var notesPost: String? = null,
    var contents: String = ""
)


class AO3Story (
    var id: Long = 0
) {
    var title: String = ""
    var authors: List<String> = listOf()
    var tags: AO3TagsInfo = AO3TagsInfo()
    var stats: AO3StatsInfo = AO3StatsInfo()
    var language: String = ""
    var complete: Boolean = false
    var summary: String? = null
    var notesPre: String? = null
    var notesPost: String? = null
    var chapters: List<AO3Chapter> = listOf()
}
