package no.nav.syfo.db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import no.nav.syfo.application.database.DatabaseInterface
import org.flywaydb.core.Flyway
import java.sql.Connection

class EmbeddedDatabase : DatabaseInterface {
    private val dataSource: HikariDataSource

    init {
        val config = HikariConfig().apply {
            jdbcUrl = "jdbc:h2:mem:lps-db;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;"
            username = "sa"
            password = ""
            maximumPoolSize = 2
            minimumIdle = 1
            isAutoCommit = false
            transactionIsolation = "TRANSACTION_REPEATABLE_READ"
            validate()
        }

        dataSource = HikariDataSource(config)

        Flyway.configure()
            .dataSource(dataSource)
            .load()
            .apply {
                migrate()
                validate()
            }
    }

    fun dropData() {
        val tables = listOf(
            "FOLLOW_UP_PLAN_LPS_V1",
            "SYKMELDINGSPERIODE",
            "ALTINN_LPS"
        )

        connection.use { connection ->
            tables.forEach { table ->
                connection.prepareStatement("DELETE FROM $table").executeUpdate()
            }
            connection.commit()
        }
    }

    override val connection: Connection
        get() = dataSource.connection.apply { autoCommit = false }
}
