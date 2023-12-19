package no.nav.syfo.api.test

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.syfo.db.DatabaseInterface
import no.nav.syfo.db.getLpsByUuid
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*

fun Routing.registerPdfApi(
    database: DatabaseInterface
) {
    val log = LoggerFactory.getLogger("Routing-logger")
    route("/api/pdf") {
        post {
            val uuid = UUID.fromString(call.request.queryParameters["uuid"])
            log.info("LPS-API :: uuid | $uuid")
            val lps = database.getLpsByUuid(uuid)
            log.info("LPS-API :: AR | ${lps.archiveReference}")
            val pdf = lps.pdf
            val pdfLen = pdf?.size
            log.info("LPS-API :: PDF byte size | $pdfLen")
            log.info("LPS")
            call.response.headers.append("Content-Type", "application/pdf")
            call.respondBytes(pdf!!, contentType = ContentType.Application.Pdf)
        }
    }
}
