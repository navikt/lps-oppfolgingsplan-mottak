package no.nav.syfo.client.krrproxy.domain

data class PostPersonerRequest(val personidenter: Set<String>) {
    companion object {
        fun createForFnr(fnr: String): PostPersonerRequest = PostPersonerRequest(personidenter = setOf(fnr))
    }
}
