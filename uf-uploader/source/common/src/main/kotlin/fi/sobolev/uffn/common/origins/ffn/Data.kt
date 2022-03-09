package fi.sobolev.uffn.common.origins.ffn

import java.time.LocalDate


enum class FFNRating {
    Kids,
    KidsPlus,
    Teens,
    Mature;

    companion object {
        fun fromString(str: String): FFNRating = when (str.uppercase()) {
            "K" -> Kids
            "K+" -> KidsPlus
            "T" -> Teens
            "M" -> Mature
            else -> throw Exception("unrecognised ff.net rating")
        }
    }

    override fun toString(): String = when (this) {
        Kids -> "K"
        KidsPlus -> "K+"
        Teens -> "T"
        Mature -> "M"
    }
}


enum class FFNGenre {
    Adventure,
    Angst,
    Crime,
    Drama,
    Family,
    Fantasy,
    Friendship,
    General,
    Horror,
    Humor,
    HurtComfort,
    Mystery,
    Parody,
    Poetry,
    Romance,
    SciFi,
    Spiritual,
    Supernatural,
    Suspense,
    Tragedy,
    Western;

    companion object {
        val testStrings = listOf (
            "Adventure", "Angst", "Crime", "Drama", "Family",
            "Fantasy", "Friendship", "General", "Horror", "Humor",
            "Hurt", "Comfort", "Mystery", "Parody", "Poetry",
            "Romance", "Sci-Fi", "Spiritual", "Supernatural", "Suspense",
            "Tragedy", "Western"
        )

        fun fromString(str: String): FFNGenre {
            if (str.equals("Hurt/Comfort", ignoreCase = true)) {
                return HurtComfort
            }

            if (str.equals("Sci-Fi", ignoreCase = true)) {
                return SciFi
            }

            values().map { Pair(it, it.toString()) }.forEach { (genre, name) ->
                if (name.equals(str, ignoreCase = true)) {
                    return genre
                }
            }

            throw Exception("unrecognised ff.net genre")
        }
    }

    override fun toString(): String = when (this) {
        HurtComfort -> "Hurt/Comfort"
        SciFi -> "Sci-Fi"
        else -> super.toString()
    }
}


data class FFNPairing (
    var characters: List<String>
) {
    override fun toString() = characters.joinToString(", ")
}


data class FFNChapter (
    var index: Int,
    var title: String?,
    var contents: String
) {
    val anyTitle: String
        get() = title ?: "Chapter ${index+1}"
}


class FFNStory (
    var id: Long = 0
) {
    var title: String = ""
    var author: String = ""
    var summary: String = ""
    var rating: FFNRating = FFNRating.Mature
    var language: String = ""
    var genres: List<FFNGenre> = listOf()
    var pairings: List<FFNPairing> = listOf()
    var characters: List<String> = listOf()
    var chapterCount: Long = 1
    var wordCount: Long = 0
    var reviewCount: Long = 0
    var favCount: Long = 0
    var followCount: Long = 0
    var publishedAt: LocalDate = LocalDate.MIN
    var updatedAt: LocalDate = LocalDate.MIN
    var isCompleted: Boolean = false
    var chapters: List<FFNChapter> = listOf()
}
