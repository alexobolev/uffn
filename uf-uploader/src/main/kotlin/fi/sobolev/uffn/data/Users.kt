package fi.sobolev.uffn.data

import java.time.Instant
import org.ktorm.entity.*
import org.ktorm.schema.*


interface User : Entity<User> {
    val id: Int
    var login: String
    var email: String
    var password: String
    var registeredAt: Instant
    var lastLoginAt: Instant?
    var isAdmin: Boolean
}

object Users : Table<User>("users") {
    val id = int("id").primaryKey().bindTo { it.id }
    val login = varchar("login").bindTo { it.login }
    val email = varchar("email").bindTo { it.email }
    val password = varchar("password").bindTo { it.password }
    val registeredAt = timestamp("registered_at").bindTo { it.registeredAt }
    val loginAt = timestamp("login_at").bindTo { it.lastLoginAt }
    val isAdmin =  boolean("is_admin").bindTo { it.isAdmin }
}
