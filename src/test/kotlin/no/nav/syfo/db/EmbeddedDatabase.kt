package no.nav.syfo.db

import com.opentable.db.postgres.embedded.EmbeddedPostgres
import no.nav.syfo.application.database.DatabaseInterface
import org.flywaydb.core.Flyway
import java.sql.Connection

class EmbeddedDatabase : DatabaseInterface {
    private val pg: EmbeddedPostgres = EmbeddedPostgres.start()

    override val connection: Connection
        get() = pg.postgresDatabase.connection.apply { autoCommit = false }

    init {

        Flyway.configure().run {
            dataSource(pg.postgresDatabase).load().migrate()
        }
    }

    fun stop() {
        pg.close()
    }
}
