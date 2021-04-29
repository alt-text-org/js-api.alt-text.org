package dev.hbeck.alt.text.http.auth

import com.nimbusds.jwt.JWTClaimsSet
import dev.hbeck.alt.text.http.auth.principal.PrincipalParser
import dev.hbeck.alt.text.http.auth.principal.UserPrincipal
import io.dropwizard.auth.AuthenticationException
import io.dropwizard.auth.Authenticator
import java.util.*


class AltTextAuthenticator<T : UserPrincipal>(
    private val verifier: CredentialVerifier,
    private val principalParser: PrincipalParser<T>,
    private val admins: Set<String>,
) : Authenticator<String, T> {

    override fun authenticate(credentials: String): Optional<T> {
        val claimsSet = try {
            verifier.verify(credentials)
        } catch (e: Exception) {
            throw AuthenticationException(e)
        }

        val principal = convertTokenToPrincipal(claimsSet)
        return Optional.of(principal)
    }

    private fun convertTokenToPrincipal(claimsSet: JWTClaimsSet): T {
        val roles = mutableSetOf("alttxt:USER")
        val principal = principalParser.parsePrincipal(claimsSet, roles)

        //Dirty, but the object is still immutable once it's returned.
        if (admins.contains(principal.name)) {
            roles.add("alttxt:ADMIN")
        }

        return principal
    }
}