package com.orbitalhq.spring.http.auth.schemes

import com.orbitalhq.auth.schemes.AuthScheme
import com.orbitalhq.auth.schemes.AuthSchemeProvider
import com.orbitalhq.auth.schemes.AuthTokens
import com.orbitalhq.auth.schemes.BasicAuth
import com.orbitalhq.auth.schemes.HttpHeader
import com.orbitalhq.auth.schemes.OAuth2
import com.orbitalhq.auth.schemes.QueryParam
import com.orbitalhq.schemas.ServiceName
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import reactor.core.publisher.Flux
import reactor.core.publisher.Sinks

class HoconOAuthClientRegistrationRepositoryTest {
    @Test
    fun `clears internal cache when AuthTokens are updated`() {
        val serviceName = "petflix.findAllFilms"
        val authSchemeProvider = TestAuthSchemeProvider()
        val repo = HoconOAuthClientRegistrationRepository(authSchemeProvider)
        authSchemeProvider.updateAutScheme(serviceName, OAuth2(
                "http://foo.com",
                "clientId",
                "secret",
                listOf("name", "photo"),
                OAuth2.AuthorizationGrantType.ClientCredentials
            ))

        repo.findByRegistrationId(serviceName).block()!!.providerDetails.tokenUri.shouldBe("http://foo.com")
        authSchemeProvider.updateAutScheme(serviceName, OAuth2(
            "http://bar.com",
            "clientId",
            "secret",
            listOf("name", "photo"),
            OAuth2.AuthorizationGrantType.ClientCredentials
        ))
        repo.findByRegistrationId(serviceName).block()!!.providerDetails.tokenUri.shouldBe("http://bar.com")
    }
}

private class TestAuthSchemeProvider: AuthSchemeProvider {
    val configUpdatedSink = Sinks.many().multicast().directBestEffort<AuthTokens>()
    val authSchemMap: MutableMap<ServiceName, List<AuthScheme>> = mutableMapOf()

    override fun getAuthSchemes(serviceName: ServiceName): List<AuthScheme> {
        return authSchemMap[serviceName] ?: emptyList()
    }

    override fun getAll(): Map<ServiceName, List<AuthScheme>> {
        return authSchemMap.toMap()
    }

    override val configUpdated: Flux<AuthTokens>
        get() = configUpdatedSink.asFlux()

    override fun getRegisteredKey(presentedKey: String): String? {
        return presentedKey
    }

    fun updateAutScheme(serviceName: ServiceName, authScheme: AuthScheme) {
        authSchemMap[serviceName] = listOf(authScheme)
        configUpdatedSink.tryEmitNext(AuthTokens(mapOf(serviceName to listOf(authScheme))))
    }

}