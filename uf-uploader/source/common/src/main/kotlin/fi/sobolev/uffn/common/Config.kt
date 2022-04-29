package fi.sobolev.uffn.common

import com.sksamuel.hoplite.*
import kotlin.io.path.Path
import kotlin.io.path.exists


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
        val host: String = "localhost",
        val port: Int = 5432,
        val name: String = "uffn",
        val user: String,
        val pass: String,
    )

    data class RedisConfig (
        val host: String = "localhost",
        val port: Int = 6379,
        val interval: Int = 5,
        val maxTotal: Int = 16
    )

    data class WsConfig (
        val port: Int = 7070
    )
}


/**
 * Load configuration from a cascade of config files.
 */
fun makeConfig(cli: Array<String>) : Config {
    val cwd = Path("").toAbsolutePath()
//    println("DEBUG: cwd = $cwd")

    return ConfigLoaderBuilder.default()
        .addCommandLineSource(cli)
        .addEnvironmentSource(useUnderscoresAsSeparator = true, allowUppercaseNames = true)
        .addPathSource(cwd.resolve("uf-uploader.yml"), optional = true)
        .addPathSource(cwd.resolve("uf-uploader.dev.yml"), optional = true)
        .addPathSource(cwd.resolve("uf-uploader.prod.yml"), optional = true)
        .build()
        .loadConfigOrThrow<Config>()
}


/**
 * Global app-wide configuration instance.
 * Set during app initialization.
 */
lateinit var gAppConfig: Config
