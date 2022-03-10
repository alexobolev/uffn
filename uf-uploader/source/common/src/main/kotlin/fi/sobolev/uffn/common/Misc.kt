package fi.sobolev.uffn.common

import java.util.UUID


fun String.tryParseUuid(): UUID? {
    return try {
        UUID.fromString(this)
    } catch (ex: IllegalArgumentException) {
        null
    }
}
