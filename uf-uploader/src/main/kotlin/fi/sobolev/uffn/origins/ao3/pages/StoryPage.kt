package fi.sobolev.uffn.origins.ao3.pages

import fi.sobolev.uffn.origins.ao3.*
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.time.LocalDate
import java.time.format.DateTimeFormatter


data class AO3WorkMeta (
    var ratings: List<AO3Rating> = listOf(),
    var warnings: List<AO3Warning> = listOf(),
    var categories: List<AO3Category> = listOf(),
    var fandoms: List<String> = listOf(),
    var relationships: List<AO3Relationship> = listOf(),
    var characters: List<String> = listOf(),
    var freeTags: List<String> = listOf(),
    var language: String = "",
    var publishedAt: LocalDate = LocalDate.MIN,
    var updatedAt: LocalDate = LocalDate.MIN,
    var chaptersDone: Long = 0,
    var chaptersPlanned: Long? = 0,
    var wordCount: Long = 0,
    var commentCount: Long = 0,
    var kudoCount: Long = 0,
    var bookmarkCount: Long = 0,
    var hitCount: Long = 0,
    var complete: Boolean = false
) {
    fun applyTo(story: AO3Story): AO3WorkMeta {
        story.also {
            it.language = language
            it.complete = complete
        }
        story.tags.also { tags ->
            tags.ratings = ratings
            tags.warnings = warnings
            tags.categories = categories
            tags.fandoms = fandoms
            tags.relationships = relationships
            tags.characters = characters
            tags.freeform = freeTags
        }
        story.stats.also { stats ->
            stats.publishedAt = publishedAt
            stats.updatedAt = updatedAt
            stats.chaptersDone = chaptersDone
            stats.chaptersPlanned = chaptersPlanned
            stats.wordCount = wordCount
            stats.commentCount = commentCount
            stats.kudoCount = kudoCount
            stats.bookmarkCount = bookmarkCount
            stats.hitCount = hitCount
        }
        return this
    }
}

data class AO3WorkInfo (
    var title: String = "",
    var authors: List<String> = listOf(),
    var summary: String? = null,
    var preface: String? = null,
    var afterword: String? = null
) {
    fun applyTo(story: AO3Story): AO3WorkInfo {
        story.title = title
        story.authors = authors
        story.summary = summary
        story.notesPre = preface
        story.notesPost = afterword
        return this
    }
}

data class AO3ChapterData (
    var id: Long = 0,
    var title: String? = null,
    var summary: String? = null,
    var preface: String? = null,
    var afterword: String? = null,
    var contents: String = ""
) {
    fun applyTo(chapter: AO3Chapter): AO3ChapterData {
        chapter.id = id
        chapter.title = title
        chapter.summary = summary
        chapter.notesPre = preface
        chapter.notesPost = afterword
        chapter.contents = contents
        return this
    }
}


class AO3StoryPage (
    html: String
) {
    private val document: Document = Jsoup.parse(html)

    private val mainBlock: Element = document.getElementById("main") ?: throw Exception("can't find #main block")
    private val metaBlock: Element = mainBlock.selectFirst("dl.work.meta.group") ?: throw Exception("can't find work meta group block")
    private val workBlock: Element = mainBlock.getElementById("workskin") ?: throw Exception("can't find #workskin block")
    private val chapBlock: Element = workBlock.getElementById("chapters") ?: throw Exception("can't find #chapters block")

    private lateinit var statBlock: Element


    companion object {
        private val chapterLinkRegex = """^/works/(\d+)/chapters/(\d+)$""".toRegex()
        private val chapterCountRegex = """^(\d+)\s*\/\s*(\d+|\?)${'$'}""".toRegex()
        private fun parseChapterCounts(text: String): Pair<Long, Long?> =
            chapterCountRegex.matchEntire(text.trim())?.let {
                Pair(it.groupValues[1].toLong(), it.groupValues[2].toLongOrNull())
            } ?: throw Exception("failed to parse chapter count pair")

        private val dateFormatter = DateTimeFormatter.ofPattern("uuuu-MM-dd")
        private fun parseDate(text: String) = LocalDate.parse(text, dateFormatter)
    }


    fun getWorkMeta(): AO3WorkMeta {
        val meta = AO3WorkMeta()

        with (meta) {
            ratings = getTags("rating", required = true).map { AO3Rating.fromString(it) }
            warnings = getTags("warning", required = true).map { AO3Warning.fromString(it) }
            categories = getTags("category").map { AO3Category.fromString(it) }
            fandoms = getTags("fandom", required = true)
            relationships = getTags("relationship").map { AO3Relationship.fromString(it) }
            characters = getTags("character")
            freeTags = getTags("freeform")
        }

        meta.language = metaBlock.selectFirst("dd.language")?.ownText()
            ?: throw Exception("can't find language meta definition")

        with (meta) {
            publishedAt = getStat("published")?.let { parseDate(it) } ?: throw Exception("can't find publish date stat")
            updatedAt = getStat("status")?.let { parseDate(it) } ?: publishedAt
            wordCount = getStat("words")?.toLong() ?: throw Exception("can't find words stat")
            commentCount = getStat("comments")?.toLong() ?: 0
            kudoCount = getStat("kudos")?.toLong() ?: 0
            bookmarkCount = getStat("bookmarks")?.toLong() ?: 0
            hitCount = getStat("hits")?.toLong() ?: 0

            getStat("chapters")?.let { parseChapterCounts(it) }?.let {
                chaptersDone = it.first
                chaptersPlanned = it.second
            } ?: throw Exception("can't find chapter counts pair")

            val statusElem = statBlock.selectFirst("dt.status")
            complete = statusElem?.ownText()?.startsWith("completed", ignoreCase = true)
                ?: (chaptersDone == chaptersPlanned && chaptersDone == 1L)
        }

        return meta
    }
    fun getWorkInfo(): AO3WorkInfo {
        val info = AO3WorkInfo()

        workBlock.selectFirst("div.preface.group")
            ?.let { PrefaceGroup.parseFrom(it) }
            ?.let {
                with (info) {
                    title = it.title?.ownText() ?: throw Exception("can't find work title in the preface")
                    authors = it.byline?.select("a[rel=author]")?.eachText() ?: throw Exception("can't find work authors in the preface")
                    summary = it.summary?.html()
                    preface = it.notes?.html()
                }
            } ?: throw Exception("can't find the titular work preface group")

        workBlock.selectFirst("div.afterword.preface.group")
            ?.let { PrefaceGroup.parseFrom(it) }
            ?.let { info.afterword = it.notes?.html() }

        return info
    }
    fun getChapters(): List<AO3ChapterData> {
        val cleanupContents = { article: Element ->
            article.also {
                it.selectFirst("h3#work.landmark.heading")?.remove()
                it.select("p").removeIf { paragraph -> paragraph.text().isBlank() }
            }
        }

        return chapBlock.children().mapIndexed { index, element ->
            check (element.`is`("div.chapter[id=\"chapter-${index + 1}\"]"))
            check (element.children().size in 1 .. 3)

            val preface = element.child(0).let { PrefaceGroup.parseFrom(it) }
            val afterword = element.children().getOrNull(2)?.let { PrefaceGroup.parseFrom(it) }

            val contents = element.child(1).let { article ->
                check (article.`is`("div.userstuff.module[role=article]"))
                return@let article.let(cleanupContents).html()
            }

            return@mapIndexed AO3ChapterData (
                id = preface.title?.selectFirst("a[href]")?.attr("href")?.let { link ->
                    val match = chapterLinkRegex.matchEntire(link) ?: throw Exception("can't match chapter link")
                    return@let match.groupValues[2].toLong()
                } ?: throw Exception("can't find chapter title/id"),
                title = preface.title?.ownText()?.trim(' ', ':')?.ifBlank { null },
                summary = preface.summary?.html(),
                preface =  preface.notes?.html(),
                afterword = afterword?.notes?.html(),
                contents = contents
            )
        }
    }


    private fun getTags(className: String, required: Boolean = false): List<String> {
        val listElem = metaBlock.selectFirst("dd.${className}.tags>ul")
        val childTexts = listElem?.children()?.map { it.text() } ?: listOf()

        return childTexts.also {
            if (required && it.isEmpty()) {
                throw Exception("can't find $className tag listing")
            }
        }
    }
    private fun getStat(className: String): String? {
        if (!::statBlock.isInitialized) {
            statBlock = metaBlock.selectFirst("dd.stats>dl.stats")
                ?: throw Exception("can't find stats meta definition")
        }
        val defnElem = statBlock.selectFirst("dd.$className")
        return defnElem?.text()?.trim()
    }


    private data class PrefaceGroup (
        var title: Element? = null,
        var byline: Element? = null,
        var summary: Element? = null,
        var notes: Element? = null
    ) {
        companion object {
            fun parseFrom(elem: Element) = PrefaceGroup (
                title = elem.selectFirst(".title"),
                byline = elem.selectFirst(".byline"),
                summary = elem.selectFirst(".summary > blockquote.userstuff"),
                notes = elem.selectFirst(".notes > blockquote.userstuff")
            )
        }
    }
}
