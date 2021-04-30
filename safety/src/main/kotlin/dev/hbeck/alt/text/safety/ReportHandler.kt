package dev.hbeck.alt.text.safety

import dev.hbeck.alt.text.proto.AltTextCoordinate
import dev.hbeck.alt.text.proto.SubmittingUserInfo


interface ReportHandler {
    fun handleReport(coordinate: AltTextCoordinate, submittingUserInfo: SubmittingUserInfo, reason: String): Boolean
}