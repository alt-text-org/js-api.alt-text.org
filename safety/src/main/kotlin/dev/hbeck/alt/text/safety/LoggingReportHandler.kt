package dev.hbeck.alt.text.safety

import dev.hbeck.alt.text.proto.AltTextCoordinate
import dev.hbeck.alt.text.proto.SubmittingUserInfo
import mu.KotlinLogging

private val log = KotlinLogging.logger {}

class LoggingReportHandler : ReportHandler {
    override fun handleReport(
        coordinate: AltTextCoordinate,
        submittingUserInfo: SubmittingUserInfo,
        reason: String
    ): Boolean {
        log.info { "Got report! Submitting user: $submittingUserInfo coordinate: $coordinate reason: '$reason'" }
        return true
    }
}