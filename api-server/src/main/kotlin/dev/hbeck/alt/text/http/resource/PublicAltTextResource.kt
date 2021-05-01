package dev.hbeck.alt.text.http.resource

import com.google.inject.name.Named
import dev.hbeck.alt.text.AltTextConfiguration
import dev.hbeck.alt.text.http.auth.principal.UserPrincipal
import dev.hbeck.alt.text.http.ratelimits.RateLimitScopeExtractor.IP
import dev.hbeck.alt.text.http.ratelimits.RateLimitScopeExtractor.USER
import dev.hbeck.alt.text.http.ratelimits.RateLimited
import dev.hbeck.alt.text.proto.*
import dev.hbeck.alt.text.hashing.Hasher
import dev.hbeck.alt.text.mutation.UserActionHandler
import dev.hbeck.alt.text.ocr.OcrManager
import dev.hbeck.alt.text.retriever.MatchManager
import dev.hbeck.alt.text.storage.AltTextRetriever
import dev.hbeck.alt.text.storage.firestore.UsageMarker
import io.dropwizard.auth.Auth
import java.net.URL
import java.time.Instant
import java.time.format.DateTimeParseException
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
    @Named("queuingHandler") private val userActionHandler: UserActionHandler,
    private val usageMarker: UsageMarker,
    private val retriever: AltTextRetriever,
    private val matchManager: MatchManager,
    private val ocrManager: OcrManager,
    private val hasher: Hasher,
    private val config: AltTextConfiguration
) {

    companion object {
        private const val intensityHistHeader = "X-Alt-Text-Org-Intensity-Hist"
        private const val timestampHeader = "X-Alt-Text-Org-Timestamp"
        private const val maxMatches = "20"
        private const val intensityHistByteLength = 400

        private val maxMatchesInt = maxMatches.toInt()
    }

    @Context
    private lateinit var securityContext: SecurityContext

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/search/img/{image_hash}/{language}")
    @RateLimited("GET_ALT_TEXT", 0.20, IP)
    fun search(
        @PathParam("image_hash") imageHash: String,
        @PathParam("language") language: String,
        @DefaultValue(maxMatches) @QueryParam("matches") matches: Int,
        @DefaultValue("") @QueryParam("ocr_url") ocrUrl: String,
        @DefaultValue("") @HeaderParam(intensityHistHeader) intensityHist: String
    ): GetAltTextsForImageResponse {
        if (matches > maxMatchesInt) {
            throw BadRequestException("""{"reason": "Only $maxMatches may be requested"}""")
        } else if (matches <= 0) {
            throw BadRequestException("""{"reason": "Query parameter 'matches' must be in [1, $maxMatches]"}""")
        }

        ocrUrl.takeIf { it.isNotEmpty() }?.also {
            try {
                URL(it)
            } catch (e: Exception) {
                throw BadRequestException("""{"reason": "Malformed image URL"}""")
            }
        }

        val heuristics = if (intensityHist.isNotEmpty()) {
            Heuristics(intensityHist = intensityHist)
        } else {
            null
        }

        val texts = matchManager.getMatchingTexts(
            imageHash = checkHash("image_hash", imageHash),
            heuristics = heuristics,
            language = checkLanguage(language),
            matches = matches,
            includePrivate = false
        )

        if (texts.isEmpty()) {
            throw NotFoundException()
        }

        val extracted = if (ocrUrl.isNotEmpty()) {
            ocrManager.attemptOcr(ocrUrl)
        } else {
            listOf()
        }

        return GetAltTextsForImageResponse(
            texts = texts,
            extractedText = extracted
        )
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/img/{image_hash}/{language}")
    @RateLimited("ADD_ALT_TEXT", 0.1, USER)
    fun addAltText(
        @PathParam("image_hash") imageHash: String,
        @PathParam("language") language: String,
        @DefaultValue("") @HeaderParam(timestampHeader) timestampStr: String,
        @DefaultValue("") @HeaderParam(intensityHistHeader) intensityHist: String,
        @Auth user: UserPrincipal,
        altText: NewAltText
    ): Response {
        altText.url.takeIf { it.isNotEmpty() }?.let {
            try {
                URL(it)
            } catch (e: Exception) {
                throw BadRequestException("""{"reason": "Malformed image URL"}""")
            }
        }

        val text = altText.text.trim()

        if (text.length < config.minTextLength || text.length > config.maxTextLength) {
            throw BadRequestException("""{"reason": "Text length must be in [${config.minTextLength}, ${config.maxTextLength}]"}""")
        }


        val heuristics = if (intensityHist.isNotEmpty()) {
            try {
                Base64.getDecoder().decode(intensityHist)
            } catch (e: Exception) {
                null
            }?.takeIf { it.size == intensityHistByteLength }
                ?: throw BadRequestException("""{"reason": "Malformed intensity histogram, expected $intensityHistByteLength base64 bytes"}""")

            Heuristics(intensityHist = intensityHist)
        } else {
            Heuristics()
        }

        val event = UserActionEvent(
            coordinate = AltTextCoordinate(
                imageHash = checkHash("image_hash", imageHash),
                userHash = hasher.hash(user.name),
                language = checkLanguage(language)
            ),
            userInfo = user.toSubmittingUser(),
            timestamp = parseTimestamp(timestampStr),
            event = UserActionEvent.Event.NewAltText(
                NewAltTextEvent(
                    text = altText.text,
                    url = altText.url,
                    public = altText.public,
                    heuristics = heuristics
                )
            )
        )

        if (userActionHandler.handleEvent(event)) {
            return Response.accepted().build()
        } else {
            throw ServiceUnavailableException()
        }
    }

    @DELETE
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/img/{image_hash}/{language}")
    @RateLimited("DEL_ALT_TEXT", 0.1, USER)
    fun deleteAltText(
        @PathParam("image_hash") imageHash: String,
        @PathParam("language") language: String,
        @DefaultValue("") @HeaderParam(timestampHeader) timestampStr: String,
        @Auth user: UserPrincipal
    ): Response {
        val event = UserActionEvent(
            coordinate = AltTextCoordinate(
                imageHash = checkHash("image_hash", imageHash),
                userHash = hasher.hash(user.name),
                language = checkLanguage(language)
            ),
            userInfo = user.toSubmittingUser(),
            timestamp = parseTimestamp(timestampStr),
            event = UserActionEvent.Event.Delete(
                DeleteEvent()
            )
        )

        if (userActionHandler.handleEvent(event)) {
            return Response.accepted().build()
        } else {
            throw ServiceUnavailableException()
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/user")
    @RateLimited("USER_ALT_TEXT", 0.1, USER)
    fun getUser(@Auth user: UserPrincipal): GetAltTextsForUserResponse {
        val altTextForUser = retriever.getTextsForUser(hasher.hash(user.name))
            .map {
                UserAltText(
                    imageHash = it.coordinate!!.imageHash,
                    language = it.coordinate!!.language,
                    text = it.text,
                    url = it.url,
                    timesUsed = it.timesUsed,
                    public = it.isPublic
                )
            }

        if (altTextForUser.isNotEmpty()) {
            return GetAltTextsForUserResponse(altTextForUser)
        } else {
            throw NotFoundException()
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/report/{image_hash}/{user_hash}/{language}")
    @RateLimited("REPORT_ALT_TEXT", 0.03, USER)
    fun report(
        @PathParam("image_hash") imageHash: String,
        @PathParam("user_hash") userHash: String,
        @PathParam("language") language: String,
        @Auth user: UserPrincipal,
        report: AltTextReport
    ): Response {
        if (report.reason.isBlank()) {
            throw BadRequestException("""{"reason": "No reason provided"}""")
        }

        val event = UserActionEvent(
            coordinate = AltTextCoordinate(
                imageHash = checkHash("image_hash", imageHash),
                userHash = checkHash("user_hash", userHash),
                language = checkLanguage(language)
            ),
            userInfo = user.toSubmittingUser(),
            timestamp = System.currentTimeMillis(),
            event = UserActionEvent.Event.ReportAltText(
                AltTextReportEvent(
                    reason = report.reason
                )
            )
        )

        if (userActionHandler.handleEvent(event)) {
            return Response.accepted().build()
        } else {
            throw ServiceUnavailableException()
        }
    }

    @POST
    @Path("/mark/{image_hash}/{user_hash}/{language}")
    @RateLimited("MARK_ALT_TEXT", 1.0, IP)
    fun markAltTextUsed(
        @PathParam("image_hash") imageHash: String,
        @PathParam("user_hash") userHash: String,
        @PathParam("language") language: String,
    ): Response {
        val coordinate = AltTextCoordinate(
            imageHash = checkHash("image_hash", imageHash),
            userHash = checkHash("user_hash", userHash),
            language = checkLanguage(language)
        )

        usageMarker.markUsage(coordinate)

        return Response.accepted().build()
    }

    private val hashRegex = "^[0-9a-fA-F]{64}$".toRegex()
    private fun checkHash(fieldName: String, maybeHash: String): String {
        if (!hashRegex.matches(maybeHash)) {
            throw BadRequestException("""{"reason": "Malformed hash '$maybeHash' for field $fieldName"}""")
        }

        return maybeHash
    }

    private fun parseTimestamp(ts: String): Long {
        if (ts.isEmpty()) {
            throw BadRequestException("""{"reason": "Missing or empty required header: '$timestampHeader'"}""")
        }

        try {
            return Instant.parse(ts).toEpochMilli()
        } catch (e: DateTimeParseException) {
            throw BadRequestException("""{"reason": "Malformed $timestampHeader header: ${ts}. Expected ISO-8601 millisecond precision UTC timestamp"}""")
        }
    }

    private fun checkLanguage(language: String): String {
        if (language.length != 2 || Locale.forLanguageTag(language) == null) {
            throw BadRequestException("""{"reason": "Malformed or missing ISO 639-2 language code"}""")
        }

        return language
    }
}
