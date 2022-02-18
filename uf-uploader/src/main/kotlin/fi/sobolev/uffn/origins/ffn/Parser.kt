package fi.sobolev.uffn.origins.ffn

import fi.sobolev.uffn.fetching.*
import fi.sobolev.uffn.origins.ffn.pages.*
import java.time.Duration
import org.openqa.selenium.By
import org.openqa.selenium.support.ui.*


/**
 * Autonomous class which retrieves & parses an entire ff.net story by its ID.
 */
class FFNParser (
    private val storyId: Long,
    private val browser: Browser,
    private val baseUrl: String = "https://www.fanfiction.net"
) {
    private val requestTimeout: Long = 10  // (seconds)
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
            }

            story.chapters[chapterIndex].contents = page.getChapterContents()
            chapterIndex += 1
        } while (chapterIndex < chapterCount)

        return story
    }

    private fun getHtml(url: String): String = when (browser) {
        is SeleniumBrowser -> {
            browser.getContents(url) { driver ->
                val timeout = Duration.ofSeconds(requestTimeout)
                WebDriverWait(driver, timeout).also { wait ->
                    wait.until(ExpectedConditions.presenceOfElementLocated(By.id("review_name_value")))
                }
            }
        }
        else -> {
            browser.getContents(url)
        }
    }
}
