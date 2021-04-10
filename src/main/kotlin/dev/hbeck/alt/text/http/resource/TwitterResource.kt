package dev.hbeck.alt.text.http.resource

import dev.hbeck.alt.text.http.ratelimits.RateLimitScopeExtractor
import dev.hbeck.alt.text.http.ratelimits.RateLimited
import dev.hbeck.alt.text.proto.TwitterAnalysis
import dev.hbeck.alt.text.twitter.TwitterAnalyzer
import javax.annotation.security.PermitAll
import javax.inject.Inject
import javax.inject.Singleton
import javax.ws.rs.GET
import javax.ws.rs.NotFoundException
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Singleton
@Path("/api/twitter/v1")
@PermitAll
class TwitterResource @Inject constructor(private val analyzer: TwitterAnalyzer) {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/recent-alt-text-usage/{username}")
    @RateLimited("TWITTER_ANALYZE", 0.033, RateLimitScopeExtractor.USER)
    fun getAltTextForImage(
        @PathParam("username") username: String
    ): TwitterAnalysis {
        return analyzer.analyze(username.substringAfter("@")) ?: throw NotFoundException()
    }
}