package fi.sobolev.uffn

import org.apache.commons.dbcp2.BasicDataSource
import org.ktorm.database.Database
import org.ktorm.support.postgresql.PostgreSqlDialect


lateinit var DbConn: Database


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
