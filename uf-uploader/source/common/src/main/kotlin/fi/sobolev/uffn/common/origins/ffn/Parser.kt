package fi.sobolev.uffn.common.origins.ffn

import fi.sobolev.uffn.common.fetching.*
import fi.sobolev.uffn.common.origins.ffn.pages.*
import mu.KotlinLogging


private val logger = KotlinLogging.logger {}

/**
 * Autonomous class which retrieves & parses an entire ff.net story by its ID.
 */
class FFNParser (
    private val storyId: Long,
    private val browser: Browser,
    private val baseUrl: String = "https://www.fanfiction.net"
) {
    private val requestTimeout: Long = 15  // (seconds)
    private val requestDelay: Long = 2  // (seconds)

    fun parse(): FFNStory {
        val story = FFNStory(id = storyId)

        var chapterIndex = 0
        var chapterCount = 0

        do {
            val url = "${baseUrl}/s/${story.id}/${chapterIndex + 1}"
            val page = FFNChapterPage(html = getHtml(url))

            if (chapterIndex == 0) {
                with (page) {
                    getStoryInfo().applyTo(story)
                    getStoryMetadata().applyTo(story)
                    getTableOfContents().applyTo(story).also {
                        check (it.selectedIndex == 0)
                        chapterCount = it.titles.size
                    }
                }

                logger.info { "story: ${story.title} by ${story.author} with ${story.chapterCount} chapter(s)"}
            }

            story.chapters[chapterIndex].contents = page.getChapterContents()
            chapterIndex += 1

            logger.info { "parsed ffn chapter $chapterIndex for story $storyId" }

            Thread.sleep(requestDelay * 1000)
        } while (chapterIndex < chapterCount)

        return story
    }

    private fun getHtml(url: String): String
        = browser.getContents(url)
}
