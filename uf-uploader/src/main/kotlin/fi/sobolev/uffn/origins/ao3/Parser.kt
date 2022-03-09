package fi.sobolev.uffn.origins.ao3

import fi.sobolev.uffn.fetching.*
import fi.sobolev.uffn.origins.ao3.pages.AO3StoryPage


class AO3Parser (
    private val storyId: Long,
    private val browser: Browser,
    private val baseUrl: String = "https://archiveofourown.org"
) {
    private val requestTimeout: Long = 10  // (seconds)
    private val requestDelay: Long = 2  // (seconds)

    fun parse(): AO3Story {
        val story = AO3Story(id = storyId)

        val url = "${baseUrl}/works/${story.id}?view_full_work=true"
        val page = AO3StoryPage(html = browser.getContents(url))

        with (page) {
            getWorkMeta().applyTo(story)
            getWorkInfo().applyTo(story)

            story.chapters = getChapters().mapIndexed { i, data ->
                AO3Chapter(index = i.toLong()).also { data.applyTo(it) }
            }
        }

        return story
    }
}
