package dev.hbeck.alt.text.http.auth

import com.nimbusds.jose.proc.SecurityContext
import io.dropwizard.auth.AuthFilter
import io.dropwizard.auth.chained.ChainedAuthFilter


class MultiAuthFilter(filters: List<AuthFilter<*, *>>) :
    ChainedAuthFilter<SecurityContext, UserPrincipal>(filters) {
}