package fi.sobolev.uffn.common.server

import fi.sobolev.uffn.common.data.*
import fi.sobolev.uffn.common.fetching.*
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
    val created: MutableList<UploadEntry> = mutableListOf(),
    val failed: MutableList<UploadFailedEntry> = mutableListOf()
) : ServerPayload {
    override val code: String get() = "upload.created"

    fun addCreated(upload: Upload) {
        val entry = UploadEntry (
            guid = upload.guid,
            url = StoryUrl.make(upload.archive, upload.identifier),
            origin = UploadEntry.Origin(upload.archive, upload.identifier),
            status = upload.status,
            title = upload.title,
            timestamp = upload.startedAt,
            logs = listOf()
        )
        created.add(entry)
    }
    fun addFailed(url: String, reason: String) {
        val entry = UploadFailedEntry(url, reason)
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
    val removed: MutableList<RemovedEntry> = mutableListOf(),
    val failed: MutableList<ErrorEntry> = mutableListOf()
) : ServerPayload {
    override val code: String get() = "upload.removed"

    @Serializable
    data class RemovedEntry (
        val guid: String,
    )

    @Serializable
    data class ErrorEntry (
        val guid: String,
        val reason: String
    )

    fun addRemoved(guid: String) {
        removed.add(RemovedEntry(guid))
    }
    fun addFailed(guid: String, why: String) {
        failed.add(ErrorEntry(guid, reason = why))
    }
}


@Serializable
class UploadGetLogsRequest (
    @Serializable(with = UUIDSerializer::class)
    val guid: UUID
) : ClientPayload {
    override val code: String get() = "upload.get-logs"
}

@Serializable
class UploadInfoResponse (
    val uploads: MutableList<UploadEntry> = mutableListOf()
) : ServerPayload {
    override val code: String get() = "upload.info"

    fun addUpload(upload: Upload, logs: List<UploadLog>) {
        val entry = UploadEntry (
            guid = upload.guid,
            url = StoryUrl.make(upload.archive, upload.identifier),
            origin = UploadEntry.Origin(upload.archive, upload.identifier),
            status = upload.status,
            title = upload.title,
            timestamp = upload.startedAt,
            logs = logs.map {
                UploadEntry.LogEntry (
                    time = it.time,
                    level = it.level,
                    message = it.message
                )
            }
        )
        uploads.add(entry)
    }
}


// Common serializable structs.
// ====================================================

@Serializable
data class UploadEntry (
    @Serializable(with = UUIDSerializer::class)
    val guid: UUID,
    val url: String,
    val origin: Origin,
    val status: UploadStatus,
    val title: String?,
    @Serializable(with = InstantSerializer::class)
    val timestamp: Instant,
    val logs: List<LogEntry>
) {
    @Serializable
    data class Origin (
        val archive: Archive,
        val ident: String
    )

    @Serializable
    data class LogEntry (
        @Serializable(with = InstantSerializer::class)
        val time: Instant,
        val level: LogLevel,
        val message: String
    )
}

@Serializable
data class UploadFailedEntry (
    val url: String,
    val reason: String
)
