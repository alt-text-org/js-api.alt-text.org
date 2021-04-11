package dev.hbeck.alt.text.http.resource

import dev.hbeck.alt.text.admin.AltTextAdmin
import dev.hbeck.alt.text.http.auth.principal.UserPrincipal
import dev.hbeck.alt.text.http.ratelimits.RateLimitScopeExtractor.IP
import dev.hbeck.alt.text.http.ratelimits.RateLimitScopeExtractor.USER
import dev.hbeck.alt.text.http.ratelimits.RateLimited
import dev.hbeck.alt.text.storage.AltTextStorage
import dev.hbeck.alt.text.storage.AltTextWriteResult
import dev.hbeck.alt.text.proto.*
import dev.hbeck.alt.text.retriever.Hasher
import dev.hbeck.alt.text.retriever.MatchManager
import io.dropwizard.auth.Auth
import java.net.URL
import java.util.*
import javax.annotation.security.PermitAll
import javax.inject.Inject
import javax.inject.Singleton
import javax.ws.rs.*
import javax.ws.rs.core.Context

import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import javax.ws.rs.core.SecurityContext

@Singleton
@Path("/api/alt-text/public/v1")
@PermitAll
class PublicAltTextResource @Inject constructor(
    private val storage: AltTextStorage,
    private val matchManager: MatchManager,
    private val admin: AltTextAdmin,
    private val hasher: Hasher
) {
    companion object {
        private const val signatureHeader = "X-Alt-Text-Org-Image-Signature"
        private const val maxMatches = 10
    }

    @Context
    private lateinit var securityContext: SecurityContext

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/img/hash/{img_hash}")
    @RateLimited("GET_ALT_TEXT", 0.20, IP)
    fun getAltTextForImage(
        @PathParam("img_hash") imgHash: String,
        @QueryParam("lang") language: String,
        @QueryParam("matches") matches: Int,
        @HeaderParam(signatureHeader) signature: String
    ): GetAltTextsForImageResponse {
        if (language.length != 2 || Locale.forLanguageTag(language) == null) {
            throw BadRequestException("""{"reason": "Unknown ISO 639-2 language code: '$language'"}""")
        } else if (matches > maxMatches) {
            throw BadRequestException("""{"reason": "Only $maxMatches may be requested"}""")
        }

        val texts = matchManager.getMatchingTexts(
            imageHash = imgHash,
            signature = signature,
            language = language,
            matches = matches
        )

        if (texts.isEmpty()) {
            throw NotFoundException()
        }

        return GetAltTextsForImageResponse(
            texts = texts
        )
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/img/hash/{img_hash}")
    @RateLimited("ADD_ALT_TEXT", 0.1, USER)
    fun addAltText(
        @PathParam("img_hash") imgHash: String,
        @Auth user: UserPrincipal,
        altText: NewAltText,
    ): Response {
        altText.url.takeIf { it.isNotEmpty() }?.let {
            try {
                URL(it)
            } catch (e: Exception) {
                throw BadRequestException("""{"reason": "Malformed image URL"}""")
            }
        }

        if (altText.language.length != 2 || Locale.forLanguageTag(altText.language) == null) {
            throw BadRequestException("""{"reason": "Malformed or missing ISO 639-2 language code"}""")
        }

        val textWriteResult = storage.addAltTextAsync(
            imgHash = imgHash,
            username = user.name,
            userHash = hasher.hash(user.name),
            altText = altText.text,
            language = altText.language,
            url = altText.url.takeIf { it.isNotEmpty() })
        return when (textWriteResult) {
            AltTextWriteResult.QUEUED -> Response.accepted().build()
            AltTextWriteResult.CONFLICT -> Response.status(409, "Conflict").build()
        }
    }

    @DELETE
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/img/hash/{img_hash}")
    @RateLimited("DEL_ALT_TEXT", 0.1, USER)
    fun deleteAltText(
        @PathParam("img_hash") imgHash: String,
        @Auth user: UserPrincipal
    ): Response {
        storage.deleteAltTextAsync(imgHash = imgHash, username = user.name)
        return Response.accepted().build()
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/user")
    @RateLimited("USER_ALT_TEXT", 0.1, USER)
    fun getUser(@Auth user: UserPrincipal): GetAltTextsForUserResponse {
        val altTextForUser = storage.getAltTextForUser(user.name)
        if (altTextForUser.isNotEmpty()) {
            return GetAltTextsForUserResponse(altTextForUser)
        } else {
            throw NotFoundException()
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/favorites")
    @RateLimited("GET_FAVES", 0.1, USER)
    fun getFavorites(@Auth user: UserPrincipal): GetFavoritesForUserResponse {
        val favoritesForUser = storage.getFavoritesForUser(user.name)
        if (favoritesForUser.isNotEmpty()) {
            return GetFavoritesForUserResponse(favoritesForUser)
        } else {
            throw NotFoundException()
        }
    }

    @POST
    @Path("/favorite/{img_hash}/{user_hash}")
    @RateLimited("FAVE", 0.1, USER)
    fun favorite(
        @PathParam("img_hash") imgHash: String,
        @PathParam("user_hash") userHash: String,
        @Auth user: UserPrincipal,
        request: UserFavoriteRequest
    ): Response {
        storage.favoriteAsync(
            imgHash = imgHash,
            userHash = userHash,
            username = user.name,
            altText = request.text,
            language = request.language
        )

        return Response.accepted().build()
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/report/{img_hash}/{user_hash}")
    @RateLimited("REPORT_ALT_TEXT", 0.03, USER)
    fun report(
        @PathParam("img_hash") imgHash: String,
        @PathParam("user_hash") userHash: String,
        @Auth user: UserPrincipal,
        report: AltTextReport
    ): Response {
        if (report.reason.isBlank()) {
            throw BadRequestException("""{"reason": "No reason provided"}""")
        }

        val imageRecord = storage.getAltText(imgHash = imgHash, userHash = userHash)
        if (imageRecord != null) {
            admin.report(
                imgHash = imgHash,
                usernameHash = userHash,
                username = user.name,
                reason = report.reason
            )

            return Response.noContent().build()
        } else {
            throw NotFoundException()
        }
    }

    @POST
    @Path("/mark/{img_hash}/{user_hash}")
    @RateLimited("MARK_ALT_TEXT", 1.0, IP)
    fun markAltTextUsed(
        @PathParam("img_hash") imgHash: String,
        @PathParam("user_hash") userHash: String
    ): Response {
        storage.markAltTextUsedAsync(imgHash = imgHash, userHash = userHash)

        return Response.accepted().build()
    }
}
