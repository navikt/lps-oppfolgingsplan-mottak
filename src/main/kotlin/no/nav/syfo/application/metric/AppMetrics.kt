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
const val TAG_BISTAND_FOLLOWUP = "bistandfollowup"
const val TAG_FASTLEGE = "fastlege"
const val TAG_GOSYS = "gosys"
const val TAG_DELT = "delt"
const val TAG_DELT_FOLLOWUP = "deltfollowup"

const val METRIKK_PROSSESERING_VELLYKKET = "${METRICS_NS}_prosessering_av_lps_plan_vellykket"
const val METRIKK_DELT_MED_FASTLEGE_ETTER_FEILET_SENDING =
    "${METRICS_NS}_lps_plan_delt_med_fastlege_etter_feilet_sending"
const val METRIKK_DELT_MED_FASTLEGE = "${METRICS_NS}_lps_plan_delt_med_fastlege"
const val METRIKK_BISTAND_FRA_NAV = "${METRICS_NS}_lps_plan_behov_for_bistand_fra_nav"
const val METRIKK_DELT_MED_NAV = "${METRICS_NS}_lps_plan_delt_med_nav"

const val METRIKK_FOLLOWUP_LPS_PROSSESERING_VELLYKKET = "${METRICS_NS}_prosessering_av_followup_lps_plan_vellykket"
const val METRIKK_FOLLOWUP_LPS_DELT_MED_FASTLEGE = "${METRICS_NS}_followup_lps_plan_delt_med_fastlege"
const val METRIKK_FOLLOWUP_LPS_JOURNALFORT_TIL_GOSYS = "${METRICS_NS}_followup_lps_opprettet_journal_gosys"
const val METRIKK_FOLLOWUP_LPS_BISTAND_FRA_NAV = "${METRICS_NS}_followup_lps_plan_behov_for_bistand_fra_nav"
const val METRIKK_FOLLOWUP_LPS_DELT_MED_NAV = "${METRICS_NS}_followup_lps_plan_delt_med_nav"

val METRICS_REGISTRY =
    PrometheusMeterRegistry(PrometheusConfig.DEFAULT, CollectorRegistry.defaultRegistry, Clock.SYSTEM)

val COUNT_METRIKK_PROSSESERING_VELLYKKET: Counter = Counter
    .builder(METRIKK_PROSSESERING_VELLYKKET)
    .register(METRICS_REGISTRY)

val COUNT_METRIKK_PROSSESERING_FOLLOWUP_LPS_PROSSESERING_VELLYKKET: Counter = Counter
    .builder(METRIKK_FOLLOWUP_LPS_PROSSESERING_VELLYKKET)
    .register(METRICS_REGISTRY)

val COUNT_METRIKK_DELT_MED_FASTLEGE_ETTER_FEILET_SENDING: Counter = Counter
    .builder(METRIKK_DELT_MED_FASTLEGE_ETTER_FEILET_SENDING)
    .register(METRICS_REGISTRY)

val COUNT_METRIKK_DELT_MED_FASTLEGE: Counter = Counter
    .builder(METRIKK_DELT_MED_FASTLEGE)
    .register(METRICS_REGISTRY)

val COUNT_METRIKK_FOLLOWUP_LPS_DELT_MED_FASTLEGE_TRUE: Counter = Counter
    .builder(METRIKK_FOLLOWUP_LPS_DELT_MED_FASTLEGE)
    .tags(TAG_FASTLEGE, "true")
    .register(METRICS_REGISTRY)

val COUNT_METRIKK_FOLLOWUP_LPS_DELT_MED_FASTLEGE_FALSE: Counter = Counter
    .builder(METRIKK_FOLLOWUP_LPS_DELT_MED_FASTLEGE)
    .tags(TAG_FASTLEGE, "false")
    .register(METRICS_REGISTRY)

val COUNT_METRIKK_BISTAND_FRA_NAV_TRUE: Counter = Counter
    .builder(METRIKK_BISTAND_FRA_NAV)
    .tags(TAG_BISTAND, "true")
    .register(METRICS_REGISTRY)

val COUNT_METRIKK_FOLLOWUP_LPS_BISTAND_FRA_NAV_TRUE: Counter = Counter
    .builder(METRIKK_FOLLOWUP_LPS_BISTAND_FRA_NAV)
    .tags(TAG_BISTAND_FOLLOWUP, "true")
    .register(METRICS_REGISTRY)

val COUNT_METRIKK_BISTAND_FRA_NAV_FALSE: Counter = Counter
    .builder(METRIKK_BISTAND_FRA_NAV)
    .tags(TAG_BISTAND, "false")
    .register(METRICS_REGISTRY)

val COUNT_METRIKK_FOLLOWUP_LPS_BISTAND_FRA_NAV_FALSE: Counter = Counter
    .builder(METRIKK_FOLLOWUP_LPS_BISTAND_FRA_NAV)
    .tags(TAG_BISTAND_FOLLOWUP, "false")
    .register(METRICS_REGISTRY)

val COUNT_METRIKK_FOLLOWUP_LPS_LPS_JOURNALFORT_TIL_GOSYS_TRUE: Counter = Counter
    .builder(METRIKK_FOLLOWUP_LPS_JOURNALFORT_TIL_GOSYS)
    .tags(TAG_GOSYS, "true")
    .register(METRICS_REGISTRY)

val COUNT_METRIKK_FOLLOWUP_LPS_LPS_JOURNALFORT_TIL_GOSYS_FALSE: Counter = Counter
    .builder(METRIKK_FOLLOWUP_LPS_JOURNALFORT_TIL_GOSYS)
    .tags(TAG_GOSYS, "false")
    .register(METRICS_REGISTRY)

val COUNT_METRIKK_DELT_MED_NAV_TRUE: Counter = Counter
    .builder(METRIKK_DELT_MED_NAV)
    .tags(TAG_DELT, "true")
    .register(METRICS_REGISTRY)

val COUNT_METRIKK_DELT_MED_NAV_FALSE: Counter = Counter
    .builder(METRIKK_DELT_MED_NAV)
    .tags(TAG_DELT, "false")
    .register(METRICS_REGISTRY)

val COUNT_METRIKK_FOLLOWUP_LPS_DELT_MED_NAV_TRUE: Counter = Counter
    .builder(METRIKK_FOLLOWUP_LPS_DELT_MED_NAV)
    .tags(TAG_DELT_FOLLOWUP, "true")
    .register(METRICS_REGISTRY)

val COUNT_METRIKK_FOLLOWUP_LPS_DELT_MED_NAV_FALSE: Counter = Counter
    .builder(METRIKK_FOLLOWUP_LPS_DELT_MED_NAV)
    .tags(TAG_DELT_FOLLOWUP, "false")
    .register(METRICS_REGISTRY)

fun Routing.registerPrometheusApi() {
    DefaultExports.initialize()

    get("/prometheus") {
        call.respondText(METRICS_REGISTRY.scrape())
    }
}
