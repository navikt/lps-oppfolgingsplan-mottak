package no.nav.syfo.application.database

import java.sql.ResultSet

fun <T> ResultSet.toObject(mapper: ResultSet.() -> T): T {
    next()
    return mapper()
}

fun <T> ResultSet.toList(mapper: ResultSet.() -> T) = mutableListOf<T>().apply {
    while (next()) {
        add(mapper())
    }
}

