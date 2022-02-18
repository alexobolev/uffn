package fi.sobolev.uffn.fetching

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId


fun dateFromTimestamp(timestamp: String): LocalDate {
    return timestamp.toLong()
        .let { Instant.ofEpochSecond(it) }
        .let { LocalDate.ofInstant(it, ZoneId.of("UTC")) }
}
