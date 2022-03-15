package fi.sobolev.uffn.common.server

import java.time.*
import java.time.format.DateTimeFormatter
import java.util.*
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*


object InstantSerializer : KSerializer<Instant> {
    private val dateFormatter = DateTimeFormatter
        .ofPattern("dd/MM/uuuu HH:mm:ss")
        .withZone(ZoneId.ofOffset("", ZoneOffset.UTC))

    override val descriptor = PrimitiveSerialDescriptor("Instant", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): Instant
            = LocalDateTime.parse(decoder.decodeString(), dateFormatter).toInstant(ZoneOffset.UTC)

    override fun serialize(encoder: Encoder, value: Instant): Unit
            = encoder.encodeString(dateFormatter.format(value))
}


/**
 * @author https://stackoverflow.com/a/65398285
 */
object UUIDSerializer : KSerializer<UUID> {
    override val descriptor = PrimitiveSerialDescriptor("UUID", PrimitiveKind.STRING)
    override fun deserialize(decoder: Decoder): UUID = UUID.fromString(decoder.decodeString())
    override fun serialize(encoder: Encoder, value: UUID): Unit = encoder.encodeString(value.toString())
}
