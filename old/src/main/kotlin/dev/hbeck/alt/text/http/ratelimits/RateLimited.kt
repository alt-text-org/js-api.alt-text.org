package dev.hbeck.alt.text.http.ratelimits


@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class RateLimited(val key: String, val limit: Double, val scopeExtractor: RateLimitScopeExtractor)
