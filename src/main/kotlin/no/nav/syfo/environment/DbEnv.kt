package no.nav.syfo.environment

data class DbEnv(
        var dbHost: String,
        var dbPort: String,
        var dbName: String,
        val dbUsername: String = "",
        val dbPassword: String = "",
)
