package dev.hbeck.alt.text.http.ratelimits

import java.util.concurrent.ConcurrentHashMap
import javax.servlet.http.HttpServletRequest
import javax.ws.rs.BadRequestException
import javax.ws.rs.container.ContainerRequestContext
import javax.ws.rs.container.ContainerRequestFilter
import javax.ws.rs.container.ResourceInfo
import javax.ws.rs.core.Context


class RateLimitFilter(private val cacheSize: Int) : ContainerRequestFilter {

    private val rateLimiters = ConcurrentHashMap<String, RateLimit>()

    @Context
    lateinit var servletRequest: HttpServletRequest

    @Context
    lateinit var resourceInfo: ResourceInfo

    override fun filter(requestContext: ContainerRequestContext) {
        val limitAnnotation = resourceInfo.resourceMethod.getAnnotation(RateLimited::class.java) ?: return

        val scope = limitAnnotation.scopeExtractor.getScope(requestContext, servletRequest)
            ?: throw BadRequestException("No scope found for rate limiter")

        val limiter = rateLimiters.computeIfAbsent(limitAnnotation.key) { RateLimit(limitAnnotation.limit, cacheSize) }

        limiter.checkRateLimit(scope)
    }
}