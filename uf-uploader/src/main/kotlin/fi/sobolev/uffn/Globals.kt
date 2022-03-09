package fi.sobolev.uffn

import org.ktorm.database.Database
import redis.clients.jedis.JedisPool


lateinit var gDbConn: Database
lateinit var gRedisConn: JedisPool
