package fi.sobolev.uffn


/**
 * Encapsulation of app configuration data.
 * Members are named to clearly map to config file keys.
 */
data class Config (
    val database: PostgresConfig,
    val redis: RedisConfig,
    val websockets: WsConfig
) {
    data class PostgresConfig (
        val host: String,
        val port: Int,
        val user: String,
        val pass: String,
        val name: String
    )

    data class RedisConfig (
        val host: String,
        val port: Int,
        val interval: Int,
        val maxTotal: Int
    )

    data class WsConfig (
        val port: Int,
        val ping: Long = 10,
        val timeout: Long = 10
    )
}

/**
 * Global app-wide configuration instance.
 * Set during app initialization.
 */
lateinit var AppConfig: Config
