package dev.hbeck.alt.text.http.auth

import com.google.inject.Inject
import javax.ws.rs.NotAuthorizedException
import javax.ws.rs.container.ContainerRequestContext
import javax.ws.rs.container.ContainerRequestFilter
import javax.ws.rs.core.Response


class BannedFilter @Inject constructor(configuration: AuthConfiguration): ContainerRequestFilter {

    private val banned = configuration.banned

    override fun filter(requestContext: ContainerRequestContext) {
        val name = requestContext.securityContext?.userPrincipal?.name ?: return

        if (banned.contains(name)) {
            throw NotAuthorizedException("You are banned.", Response.status(403).build())
        }
    }
}