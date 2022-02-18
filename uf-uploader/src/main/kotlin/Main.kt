import fi.sobolev.uffn.fetching.*
import fi.sobolev.uffn.origins.ao3.AO3Parser
import fi.sobolev.uffn.origins.ffn.FFNParser


private const val BaseMockPath = "C:\\Users\\Argon\\Documents\\Projects\\pff\\data\\backend"

private val MockFFNBrowser = DiskMockBrowser (
    regex = """^https://www.fanfiction.net/s/(\d+)/(\d+)$""".toRegex(),
    resolver = {
        val story = it.groupValues[1].toLong()
        val chapter = it.groupValues[2].toLong()
        "$BaseMockPath\\ffn\\$story\\$chapter.htm"
    }
)

private val MockAO3Browser = DiskMockBrowser (
    regex = """^https://archiveofourown.org/works/(\d+)\?view_full_work=true$""".toRegex(),
    resolver = {
        val story = it.groupValues[1].toLong()
        "$BaseMockPath\\ao3\\$story\\contents.html"
    }
)


fun main(args: Array<String>) {

//    FFNParser(storyId = 13491812, browser = MockFFNBrowser).also {
//        val story = it.parse()
//    }

    AO3Parser(storyId = 23306638, browser = MockAO3Browser).also {
        val story = it.parse()
    }

}
