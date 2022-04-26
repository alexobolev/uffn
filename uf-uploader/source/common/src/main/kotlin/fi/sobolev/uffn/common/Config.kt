package fi.sobolev.uffn.common

import com.sksamuel.hoplite.ConfigLoader
import com.sksamuel.hoplite.addCommandLineSource
import com.sksamuel.hoplite.addEnvironmentSource
import com.sksamuel.hoplite.addPathSource
import kotlin.io.path.Path


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
        val port: Int
    )
}


/**
 * Load configuration from multiple sources,
 * including command line, environment,
 * and yaml configs in the working directory.
 */
fun makeConfig(cli: Array<String>) : Config {
    return ConfigLoader.Builder()
        .addCommandLineSource(cli)
        .addEnvironmentSource()
        .addPathSource(Path("uf-uploader.yml"), optional = true)
        .addPathSource(Path("uf-uploader.dev.yml"), optional = true)
        .build()
        .loadConfigOrThrow<Config>()
}


/**
 * Global app-wide configuration instance.
 * Set during app initialization.
 */
lateinit var gAppConfig: Config
