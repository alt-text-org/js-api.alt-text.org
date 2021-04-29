package dev.hbeck.alt.text.http.auth.principal

import com.nimbusds.jwt.JWTClaimsSet


class GooglePrincipalParser : PrincipalParser<GooglePrincipal> {
    override fun parsePrincipal(claimSet: JWTClaimsSet, roles: Set<String>): GooglePrincipal {
        return GooglePrincipal(roles, claimSet.subject, claimSet.getStringClaim("email").ifBlank { null })
    }
}