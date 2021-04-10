package dev.hbeck.alt.text.http.auth

import com.google.inject.AbstractModule
import com.google.inject.Provides
import com.google.inject.Singleton
import com.nimbusds.jose.proc.SecurityContext
import dev.hbeck.alt.text.http.auth.principal.GooglePrincipal
import io.dropwizard.auth.oauth.OAuthCredentialAuthFilter
import java.net.http.HttpClient


class AuthModule(
    private val configuration: AuthConfiguration
) : AbstractModule() {

    override fun configure() {
        bind(HttpClient::class.java).toInstance(HttpClient.newHttpClient())
    }

    @Provides
    @Singleton
    fun provideAuthFilter(
        resourceRetriever: BasicResourceRetriever
    ): MultiAuthFilter {
        val unauthorizedHandler = AltTextUnauthorizedHandler()

        val googleConfig = configuration.googleConfig
        val googleConfigurator = JWTProcessorConfigurator<SecurityContext>(
            googleConfig.jwkUrl,
            resourceRetriever,
            googleConfig.getAlgorithm()
        )
        val googleVerifier = CredentialVerifier(
            googleConfig.getAudience(),
            googleConfig.getIssuer(),
            configuration.acceptableTimeDeltaMillis,
            googleConfigurator
        )
        val googleAuthenticator = AltTextAuthenticator(googleVerifier, configuration.admins)
        val googleFilter = OAuthCredentialAuthFilter.Builder<GooglePrincipal>()
            .setAuthenticator(googleAuthenticator)
            .setAuthorizer(AltTextAuthorizer())
            .setPrefix("Bearer")
            .setUnauthorizedHandler(unauthorizedHandler)
            .buildAuthFilter()

        return MultiAuthFilter(listOfNotNull(googleFilter))
    }
}