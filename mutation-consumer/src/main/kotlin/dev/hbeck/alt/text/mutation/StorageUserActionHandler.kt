package dev.hbeck.alt.text.mutation

import com.google.inject.Inject
import dev.hbeck.alt.text.heuristics.HeuristicMatcher
import dev.hbeck.alt.text.heuristics.HeuristicType
import dev.hbeck.alt.text.proto.AltTextCoordinate
import dev.hbeck.alt.text.proto.InternalAltText
import dev.hbeck.alt.text.proto.NewAltTextEvent
import dev.hbeck.alt.text.proto.SubmittingUserInfo
import dev.hbeck.alt.text.proto.UserActionEvent
import dev.hbeck.alt.text.safety.ReportHandler
import dev.hbeck.alt.text.storage.AltTextMutator
import mu.KotlinLogging
import java.lang.NullPointerException

private val log = KotlinLogging.logger {}

class StorageUserActionHandler @Inject constructor(
    private val mutator: AltTextMutator,
    private val heuristicMatcher: HeuristicMatcher,
    private val reportHandler: ReportHandler
) : UserActionHandler {
    override fun handleEvent(event: UserActionEvent): Boolean {
        try {
            return when (event.event) {
                is UserActionEvent.Event.NewAltText -> handleAdd(
                    coordinate = event.coordinate!!,
                    userInfo = event.userInfo!!,
                    event = event.newAltText!!
                )
                is UserActionEvent.Event.Delete -> handleDelete(coordinate = event.coordinate!!)
                is UserActionEvent.Event.ReportAltText -> handleReport(
                    coordinate = event.coordinate!!,
                    userInfo = event.userInfo!!,
                    reason = event.reportAltText!!.reason
                )
                null -> {
                    log.error { "Inner event was null: $event" }
                    return true
                }
            }
        } catch (e: NullPointerException) {
            log.error(e) { "NPE attempting to write event: $event" }
            return true
        } catch (e: Exception) {
            log.error(e) { "Error attempting to write event: $event" }
            return false
        }
    }

    private fun handleAdd(
        coordinate: AltTextCoordinate,
        userInfo: SubmittingUserInfo,
        event: NewAltTextEvent
    ): Boolean {
        val altText = InternalAltText(
            coordinate = coordinate,
            userInfo = userInfo,
            text = event.text,
            url = event.url,
            isPublic = event.public,
            timesUsed = 0
        )

        if (!mutator.writeAltText(altText)) {
            return false
        }

        if (!event.heuristics?.intensityHist.isNullOrEmpty()) {
            return heuristicMatcher.addSignature(
                type = HeuristicType.INTENSITY_HISTOGRAM,
                coordinate = coordinate,
                signature = event.heuristics!!.intensityHist
            )
        }

        return true
    }

    private fun handleDelete(coordinate: AltTextCoordinate): Boolean {
        return mutator.deleteAltText(coordinate = coordinate)
    }

    private fun handleReport(coordinate: AltTextCoordinate, userInfo: SubmittingUserInfo, reason: String): Boolean {
        return reportHandler.handleReport(coordinate = coordinate, submittingUserInfo = userInfo, reason = reason)
    }

}