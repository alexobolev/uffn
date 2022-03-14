package fi.sobolev.uffn.common.services

import fi.sobolev.uffn.common.data.*
import org.ktorm.database.Database
import org.ktorm.dsl.*
import org.ktorm.entity.*
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime


interface ISessionService {
    fun findKey(authKey: String): SessionKeySearchResult
}

class LocalSessionService (
    private val db: Database
) : ISessionService {

    companion object {
        private val kUtcZoneId = ZoneId.of("UTC")
        private val kMyZoneId = ZoneId.systemDefault()

        private fun HACK_getProperUtcInstantFrom(dbValue: Instant): Instant {
            val systemOffset = kMyZoneId.rules.getOffset(Instant.now())
            return dbValue
                .atOffset(systemOffset)
                .atZoneSimilarLocal(kUtcZoneId)
                .toInstant()
        }
    }

    override fun findKey(authKey: String): SessionKeySearchResult {
        val session = db.sequenceOf(UploadSessions).singleOrNull { it.authKey eq authKey }
            ?: return SessionKeySearchResult(null, false)

        val expires = session.expiresAt.let { HACK_getProperUtcInstantFrom(it) }
        val rightNow = ZonedDateTime.now(kUtcZoneId).toInstant()

        return SessionKeySearchResult (
            owner = session.owner,
            active = expires > rightNow
        )
    }
}


data class SessionKeySearchResult (
    val owner: User?,
    val active: Boolean
)
