package fi.sobolev.uffn.common.services

import fi.sobolev.uffn.common.data.*
import org.ktorm.database.Database
import org.ktorm.dsl.*
import org.ktorm.entity.*


interface IUserService {
    fun findOneOrNull(id: Int): User?
}

class LocalUserService (
    private val db: Database
) : IUserService {

    override fun findOneOrNull(id: Int): User? {
        return db.sequenceOf(Users).firstOrNull{ it.id eq id }
    }
}
