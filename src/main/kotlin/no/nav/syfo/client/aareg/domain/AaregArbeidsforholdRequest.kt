package no.nav.syfo.client.aareg.domain

enum class Rapporteringsordning {
    A_ORDNINGEN,
    FOER_A_ORDNINGEN
}

data class FinnArbeidsforholdoversikterPrArbeidstakerAPIRequest(
    val arbeidstakerId: String,
    val rapporteringsordninger: Set<Rapporteringsordning> = setOf(
        Rapporteringsordning.A_ORDNINGEN,
        Rapporteringsordning.FOER_A_ORDNINGEN
    ),
)
