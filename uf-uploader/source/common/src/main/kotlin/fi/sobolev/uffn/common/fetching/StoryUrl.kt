package fi.sobolev.uffn.common.fetching

import fi.sobolev.uffn.common.data.*


object StoryUrl {

    fun parse(url: String): Pair<Archive, String>? {
        enumValues<Archive>()
            .map { archive -> Pair(archive, archive.storyLinkRegex()) }
            .forEach { (archive, regex) ->
                regex.matchEntire(url)
                    ?.let { match -> match.groups[1]?.value }
                    ?.let { ident -> return Pair(archive, ident) }
            }

        return null
    }

    fun make(archive: Archive, ident: String)
            = archive.storyLinkBasis() + ident

}


private val regexAO3 = """^https://archiveofourown\.org/works/(\d+).*$""".toRegex(RegexOption.IGNORE_CASE)
private val regexFFN = """^https://(?:(?:m|(?:www))\.)?fanfiction\.net/s/(\d+).*$""".toRegex(RegexOption.IGNORE_CASE)

fun Archive.storyLinkRegex() = when (this) {
    Archive.AO3 -> regexAO3
    Archive.FFN -> regexFFN
}

fun Archive.storyLinkBasis() = when (this) {
    Archive.AO3 -> "https://archiveofourown.org/works/"
    Archive.FFN -> "https://fanfiction.net/s/"
}
