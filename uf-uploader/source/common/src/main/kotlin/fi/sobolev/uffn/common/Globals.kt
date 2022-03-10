package fi.sobolev.uffn.common

import org.apache.commons.dbcp2.BasicDataSource
import org.ktorm.database.Database
import org.ktorm.support.postgresql.PostgreSqlDialect
import redis.clients.jedis.JedisPool
import redis.clients.jedis.JedisPoolConfig


lateinit var gDbConn: Database
lateinit var gRedisConn: JedisPool


fun makeDbConnection(config: Config.PostgresConfig): Database {
    (object : BasicDataSource() {
        init {
            driverClassName = "org.postgresql.Driver"
            url = "jdbc:postgresql://${config.host}:${config.port}/${config.name}"
            username = config.user
            password = config.pass
        }
    }).let { dataSource ->
        return Database.connect(dataSource, PostgreSqlDialect())
    }
}

fun makeRedisConnection(config: Config.RedisConfig): JedisPool {
    val poolConfig = JedisPoolConfig().apply {
        maxTotal = config.maxTotal
    }
    return JedisPool(poolConfig, config.host, config.port)
}
