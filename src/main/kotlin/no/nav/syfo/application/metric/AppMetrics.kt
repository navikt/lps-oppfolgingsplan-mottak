package no.nav.syfo.application.metric

import io.ktor.server.application.call
import io.ktor.server.response.respondText
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import io.micrometer.core.instrument.Clock
import io.micrometer.core.instrument.Counter
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import io.prometheus.client.CollectorRegistry
import io.prometheus.client.hotspot.DefaultExports

const val METRICS_NS = "lps_oppfolgingsplan_mottak"
const val TAG_BISTAND = "bistand"
const val TAG_DELT = "delt"

val METRIKK_PROSSESERING_VELLYKKET = "${METRICS_NS}_prosessering_av_lps_plan_vellykket"
val METRIKK_DELT_MED_FASTLEGE_ETTER_FEILET_SENDING = "${METRICS_NS}_lps_plan_delt_med_fastlege_etter_feilet_sending"
val METRIKK_DELT_MED_FASTLEGE = "${METRICS_NS}_lps_plan_delt_med_fastlege"
val METRIKK_LPS_JOURNALFORT_TIL_GOSYS = "${METRICS_NS}_plan_lps_opprettet_journal_gosys"
val METRIKK_BISTAND_FRA_NAV = "${METRICS_NS}_lps_plan_behov_for_bistand_fra_nav"
val METRIKK_DELT_MED_NAV = "${METRICS_NS}_lps_plan_delt_med_nav"

val METRICS_REGISTRY =
    PrometheusMeterRegistry(PrometheusConfig.DEFAULT, CollectorRegistry.defaultRegistry, Clock.SYSTEM)

val COUNT_METRIKK_PROSSESERING_VELLYKKET: Counter = Counter
    .builder(METRIKK_PROSSESERING_VELLYKKET)
    .register(METRICS_REGISTRY)

val COUNT_METRIKK_DELT_MED_FASTLEGE_ETTER_FEILET_SENDING: Counter = Counter
    .builder(METRIKK_DELT_MED_FASTLEGE_ETTER_FEILET_SENDING)
    .register(METRICS_REGISTRY)

val COUNT_METRIKK_DELT_MED_FASTLEGE: Counter = Counter
    .builder(METRIKK_DELT_MED_FASTLEGE)
    .register(METRICS_REGISTRY)

val COUNT_METRIKK_BISTAND_FRA_NAV_TRUE: Counter = Counter
    .builder(METRIKK_BISTAND_FRA_NAV)
    .tags(TAG_BISTAND, "true")
    .register(METRICS_REGISTRY)

val COUNT_METRIKK_BISTAND_FRA_NAV_FALSE: Counter = Counter
    .builder(METRIKK_BISTAND_FRA_NAV)
    .tags(TAG_BISTAND, "false")
    .register(METRICS_REGISTRY)

val COUNT_METRIKK_LPS_JOURNALFORT_TIL_GOSYS: Counter = Counter
    .builder(METRIKK_LPS_JOURNALFORT_TIL_GOSYS)
    .register(METRICS_REGISTRY)


val COUNT_METRIKK_DELT_MED_NAV_TRUE: Counter = Counter
    .builder(METRIKK_DELT_MED_NAV)
    .tags(TAG_DELT, "true")
    .register(METRICS_REGISTRY)

val COUNT_METRIKK_DELT_MED_NAV_FALSE: Counter = Counter
    .builder(METRIKK_DELT_MED_NAV)
    .tags(TAG_DELT, "false")
    .register(METRICS_REGISTRY)

fun Routing.registerPrometheusApi() {
    DefaultExports.initialize()

    get("/prometheus") {
        call.respondText(METRICS_REGISTRY.scrape())
    }
}
