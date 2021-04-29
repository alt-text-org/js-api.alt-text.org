package dev.hbeck.alt.text.http.ratelimits

import javax.servlet.http.HttpServletRequest
import javax.ws.rs.container.ContainerRequestContext


enum class RateLimitScopeExtractor {
    USER {
        override fun getScope(context: ContainerRequestContext, request: HttpServletRequest): String? =
            context.securityContext?.userPrincipal?.name
    },

    IP {
        override fun getScope(context: ContainerRequestContext, request: HttpServletRequest): String? =
            request.getHeaders("X-Forwarded-For").toList().lastOrNull()?.substringAfterLast(",")
                ?: request.remoteAddr
    };

    abstract fun getScope(context: ContainerRequestContext, request: HttpServletRequest): String?
}