package fi.sobolev.uffn.common.origins.ffn.pages

import fi.sobolev.uffn.common.fetching.dateFromTimestamp
import fi.sobolev.uffn.common.origins.ffn.*
import java.time.LocalDate
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element


fun parseRating(text: String): FFNRating {
    check(text.startsWith("Fiction ", ignoreCase = true))
    return text.uppercase().split(" ").last().let {
        FFNRating.fromString(it)
    }
}

fun parseDashedPairs(text: String): List<Pair<String, String>> {
    val pairSeparator = ": "
    return text.split(" - ")
        .filter { it.contains(pairSeparator) }
        .map { it.split(pairSeparator, limit = 2) }
        .onEach { check(it.size == 2) { "weird meta pair (size = ${it.size})" } }
        .map { (key, value) ->
            Pair (
                key.trim(' ', ':').uppercase(),
                value.replace(",", "")
            )
        }
}

fun parseCharacterList(text: String): Pair<List<FFNPairing>, List<String>> {
    @Suppress("RegExpUnnecessaryNonCapturingGroup", "RegExpRedundantEscape")
    val pairingRegex = """(?:\[((?:(?:[^,\[\]]+)(?:,\s)?)+)\])""".toRegex()
    val pairingMatches = pairingRegex.findAll(text)

    val pairings = pairingMatches.map {
        FFNPairing(it.groups[1]!!.value.split(", "))
    }

    val pairingsLength = if (pairings.any()) { pairingMatches.last().range.last + 1 } else { 0 }
    val characters = text.substring(startIndex = pairingsLength).let {
        if (it.isNotBlank()) {
            it.split(", ").map { str -> str.trim() }
        } else {
            listOf()
        }
    }

    return Pair(pairings.toList(), characters)
}

fun parseGenreList(text: String): Pair<Boolean, List<FFNGenre>> {
    val chunks = text.split("/").toMutableList()
    return if (FFNGenre.testStrings.containsAll(chunks)) {
        val hurtOrComfort = listOf("Hurt", "Comfort")
        with (chunks) {
            if (containsAll(hurtOrComfort)) {
                removeAll(hurtOrComfort)
                add("Hurt/Comfort")
            }
        }
        Pair(true, chunks.map { FFNGenre.fromString(it) })
    } else {
        Pair(false, listOf())
    }
}

fun parseChapterTitle(index: Int, text: String): String? = when (text) {
    "${index + 1}. Chapter ${index + 1}" -> null
    else -> text.split(". ", limit = 2).last().trim()
}


data class FFNTableOfContents (
    val titles: List<String?>,
    val selectedIndex: Int
) {
    fun applyTo(story: FFNStory): FFNTableOfContents {
        story.chapters = titles.mapIndexed { index, title ->
            FFNChapter(index, title, "")
        }
        return this
    }
}

data class FFNStoryInfo (
    val title: String,
    val author: String,
    val summary: String
) {
    fun applyTo(story: FFNStory): FFNStoryInfo {
        story.title = title
        story.author = author
        story.summary = summary
        return this
    }
}

data class FFNStoryMetadata (
    var rating: FFNRating = FFNRating.Mature,
    var language: String = "",
    var genres: List<FFNGenre> = listOf(),
    var pairings: List<FFNPairing> = listOf(),
    var characters: List<String> = listOf(),
    var chapterCount: Long = 1,
    var wordCount: Long = 0,
    var reviewCount: Long = 0,
    var favCount: Long = 0,
    var followCount: Long = 0,
    var publishedAt: LocalDate = LocalDate.MIN,
    var updatedAt: LocalDate = LocalDate.MIN,
    var isCompleted: Boolean = false,
    var id: Long = 0
) {
    fun applyTo(story: FFNStory): FFNStoryMetadata {
        story.rating = rating
        story.language = language
        story.genres = genres
        story.pairings = pairings
        story.characters = characters
        story.chapterCount = chapterCount
        story.wordCount = wordCount
        story.reviewCount = reviewCount
        story.favCount = favCount
        story.followCount = followCount
        story.publishedAt = publishedAt
        story.updatedAt = updatedAt
        story.isCompleted = isCompleted
        story.id = id
        return this
    }
}


/**
 * Semantic wrapper around low-level parsing of ff.net chapter pages.
 */
class FFNChapterPage (
    html: String
) {
    private val document: Document = Jsoup.parse(html)

    private val storyInfoElem: Element = document.getElementById("profile_top")
        ?: throw Exception("can't find story info block")
    private val storyMetaElem: Element = storyInfoElem.selectFirst("span.xgray.xcontrast_txt")
        ?: throw Exception("can't find story metadata block")
    private val chapSelectElem: Element? = document.getElementById("chap_select")
    private val chapContentsElem: Element = document.getElementById("storytext")
        ?: throw Exception("can't find chapter text block")

    fun getStoryInfo(): FFNStoryInfo {
        return FFNStoryInfo (
            title = storyInfoElem.selectFirst("b.xcontrast_txt")?.ownText()
                ?: throw Exception("can't find story title"),
            author = storyInfoElem.selectFirst("a.xcontrast_txt[href^=\"/u/\"]")?.ownText()
                ?: throw Exception("can't find story author"),
            summary = storyInfoElem.selectFirst("div.xcontrast_txt")?.ownText()
                ?: throw Exception("can't find story summary")
        )
    }

    fun getStoryMetadata(): FFNStoryMetadata {
        val meta = FFNStoryMetadata()

        val metaText = storyMetaElem.text()
        metaText.split(" - ").toMutableList().let { chunks ->
            meta.language = chunks[1].also { chunks.removeAt(1) }

            parseDashedPairs(metaText).forEach { (key, value) ->
                when (key.uppercase()) {
                    "RATED" -> { meta.rating = parseRating(value) }
                    "CHAPTERS" -> { meta.chapterCount = value.toLong() }
                    "WORDS" -> { meta.wordCount = value.toLong() }
                    "REVIEWS" -> { meta.reviewCount = value.toLong() }
                    "FAVS" -> { meta.favCount = value.toLong() }
                    "FOLLOWS" -> { meta.followCount = value.toLong() }
                    "STATUS" -> { meta.isCompleted = value.equals("Complete", ignoreCase = true) }
                    "UPDATED" -> {}
                    "PUBLISHED" -> {}
                    "ID" -> { meta.id = value.toLong() }
                    else -> { throw Exception("unrecognised meta pair (key = $key)") }
                }
            }.also {
                val keysToRemove = listOf("Rated", "Chapters", "Words", "Reviews", "Favs", "Follows", "Updated", "Published", "Status", "id")
                chunks.removeIf { chunk -> keysToRemove.any { key -> chunk.startsWith("$key: ", ignoreCase = true) } }
            }

            // At this point, only up to two chunks should be remaining, in this order:
            // genre listing (slash-separated), and character-pairing listing (which can just go die pls).

            chunks.firstOrNull()?.let {
                val (isGenreList, genres) = parseGenreList(it)
                if (isGenreList) {
                    meta.genres = genres
                    chunks.removeFirst()
                }
            }

            chunks.firstOrNull()?.let {
                val (pairings, characters) = parseCharacterList(it)
                meta.pairings = pairings
                meta.characters = characters
                chunks.removeFirst()
            }

            check(chunks.isEmpty()) { "unprocessed meta chunks remaining" }
        }

        val metaDates = storyMetaElem.select("span[data-xutime]")
        when (metaDates.size) {
            1 -> {
                meta.publishedAt = metaDates[0].attr("data-xutime").let { dateFromTimestamp(it) }
                meta.updatedAt = meta.publishedAt
            }
            2 -> {
                meta.publishedAt = metaDates[1].attr("data-xutime").let { dateFromTimestamp(it) }
                meta.updatedAt = metaDates[0].attr("data-xutime").let { dateFromTimestamp(it) }
            }
            else -> {
                throw Exception("too many or not enough timestamps in story meta")
            }
        }

        return meta
    }

    fun getTableOfContents(): FFNTableOfContents {
        return chapSelectElem?.let { select ->
            val titles = select.children().mapIndexed { index, option ->
                check(index + 1 == option.attr("value").toInt())
                return@mapIndexed parseChapterTitle(index, option.ownText())
            }
            val selected = select.children()
                .mapIndexed { i, e -> Pair(i, e) }
                .single { (_, option) -> option.hasAttr("selected") }
            return@let FFNTableOfContents(titles.toList(), selected.first)
        } ?: FFNTableOfContents(listOf(null), 0)
    }

    fun getChapterContents(): String {
        return chapContentsElem.html()
    }
}
