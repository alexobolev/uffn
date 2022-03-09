package fi.sobolev.uffn.services

import fi.sobolev.uffn.data.*
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
