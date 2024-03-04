package no.nav.syfo.util

import java.sql.Timestamp
import java.time.LocalDateTime

fun getSendingTimestamp(isSentStatus: Boolean?): Timestamp? {
    return if (isSentStatus == null || isSentStatus == false) {
        null
    } else {
        Timestamp.valueOf(LocalDateTime.now())
    }
}

