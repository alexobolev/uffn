package fi.sobolev.uffn.server

import fi.sobolev.uffn.data.User
import io.javalin.websocket.WsContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import mu.KotlinLogging


private val logger = KotlinLogging.logger {}


interface SessionRegistry {
    suspend fun hasContext(ctx: WsContext): Boolean
    suspend fun addContext(ctx: WsContext, user: User)
    suspend fun removeContext(ctx: WsContext)
    suspend fun removeUser(user: User)
    suspend fun forUser(user: User, action: (WsContext) -> Unit)
    suspend fun getUser(ctx: WsContext): User?
}


class DefaultSessionRegistry : SessionRegistry {
    private val mutex = Mutex()
    private val contexts = mutableMapOf<Int, MutableSet<WsContext>>()
    private val users = mutableMapOf<WsContext, User>()

    override suspend fun hasContext(ctx: WsContext) : Boolean {
        return mutex.withLock {
            contexts.any { it.value.contains(ctx) }
        }
    }

    override suspend fun addContext(ctx: WsContext, user: User) {
        mutex.withLock {
            users[ctx] = user
            contexts
                .getOrPut(user.id) { mutableSetOf() }
                .add(ctx)
        }
    }

    override suspend fun removeContext(ctx: WsContext) {
        mutex.withLock {
            if (users.remove(ctx) == null) {
                logger.warn { "registry is removing a non-registered context" }
            }

            contexts.forEach { it.value.remove(ctx) }
            contexts.entries.removeIf { it.value.isEmpty() }
        }
    }

    override suspend fun removeUser(user: User) {
        mutex.withLock {
            users.entries.removeIf { it.value == user }

            if (contexts.remove(user.id) == null) {
                logger.warn { "registry is removing a non-registered user instance"}
            }
        }
    }

    override suspend fun forUser(user: User, action: (WsContext) -> Unit) {
        mutex.withLock {
            contexts[user.id]?.forEach {
                action(it)
            } ?: logger.warn { "registry is iterating on a non-registered user instance" }
        }
    }

    override suspend fun getUser(ctx: WsContext): User? {
        mutex.withLock {
            return users[ctx]
        }
    }
}
