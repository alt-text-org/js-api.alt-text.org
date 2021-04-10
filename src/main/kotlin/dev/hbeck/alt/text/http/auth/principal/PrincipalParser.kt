package dev.hbeck.alt.text.http.auth.principal

import com.nimbusds.jwt.JWTClaimsSet

/**
 * Implementations must pass the roles object directly into the resulting principal without modification or copying.
 */
interface PrincipalParser<T : UserPrincipal> {
    fun parsePrincipal(claimSet: JWTClaimsSet, roles: Set<String>): T
}