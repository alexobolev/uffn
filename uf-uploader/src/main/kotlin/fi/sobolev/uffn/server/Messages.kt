package fi.sobolev.uffn.server

import fi.sobolev.uffn.data.*
import fi.sobolev.uffn.fetching.*
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import java.time.Instant
import java.util.*


@Serializable
class ClientMessage (
    val request: String,
    val payload: JsonElement
) {
    init {
        require(request.isNotBlank()) { "'request' must not be blank or empty" }
    }
}

@Serializable
class ServerMessage (
    val response: String,
    val payload: JsonElement
) {
    init {
        require(response.isNotBlank()) { "'response' field must not be blank or empty" }
    }
}


interface Payload {
    val code: String
}

interface ClientPayload : Payload
interface ServerPayload : Payload


// Generic message structs.
// ====================================================

@Serializable
class ErrorResponse (
    val reason: String,
) : ServerPayload {
    override val code: String get() = "session.error"
}


// Authentication controller messages.
// ====================================================

@Serializable
class AuthLoginRequest (
    val user: Int,
    val key: String
) : ClientPayload {
    override val code: String get() = "auth.login"
}

@Serializable
class AuthLoginSucceededResponse : ServerPayload {
    override val code: String get() = "auth.login-succeeded"
}

@Serializable
class AuthLoginFailedResponse (
    val reason: String
) : ServerPayload {
    override val code: String get() = "auth.login-failed"
}


// Upload controller messages.
// ====================================================

@Serializable
class UploadCreateRequest (
    val urls: List<String>
) : ClientPayload {
    override val code: String get() = "upload.create"
    init {
        require(urls.all { url -> url.isNotBlank() }) { "'urls' field must not contain blank or empty items" }
    }
}

@Serializable
class UploadCreatedResponse (
    val created: MutableList<UploadGoodEntry> = mutableListOf(),
    val failed: MutableList<UploadBadEntry> = mutableListOf()
) : ServerPayload {
    override val code: String get() = "upload.created"

    @Serializable
    data class UploadGoodEntry (
        @Serializable(with = UUIDSerializer::class)
        val guid: UUID,
        val url: String,
        val origin: Origin,
        val status: UploadStatus,
        val title: String?,
        @Serializable(with = InstantSerializer::class)
        val timestamp: Instant
    ) {
        @Serializable
        data class Origin (
            val archive: Archive,
            val ident: String
        )
    }

    @Serializable
    data class UploadBadEntry (
        val url: String,
        val reason: Reason
    ) {
        enum class Reason {
            URL_PROCESSING, URL_UNRECOGNIZED, URL_OTHER
        }
    }

    fun addCreated(upload: Upload) {
        val entry = UploadGoodEntry (
            guid = upload.guid,
            url = StoryUrl.make(upload.archive, upload.identifier),
            origin = UploadGoodEntry.Origin(upload.archive, upload.identifier),
            status = upload.status,
            title = upload.title,
            timestamp = upload.startedAt
        )
        created.add(entry)
    }

    fun addUnrecognized(url: String) {
        val entry = UploadBadEntry(url, UploadBadEntry.Reason.URL_UNRECOGNIZED)
        failed.add(entry)
    }

    fun addProcessing(url: String) {
        val entry = UploadBadEntry(url, UploadBadEntry.Reason.URL_PROCESSING)
        failed.add(entry)
    }
}

@Serializable
class UploadRemoveRequest (
    val guids: List<String>
) : ClientPayload {
    override val code: String get() = "upload.remove"
    init {
        require(guids.all { guid -> guid.isNotBlank() }) { "'guids' field must not contain blank or empty items" }
    }
}

@Serializable
class UploadRemovedResponse (
    @Serializable(with = UUIDSerializer::class)
    val guid: UUID
) : ServerPayload {
    override val code: String get() = "upload.created"
}


@Serializable
class UploadGetLogsRequest (
    @Serializable(with = UUIDSerializer::class)
    val guid: UUID
) : ClientPayload {
    override val code: String get() = "upload.get-logs"
}
