package dev.hbeck.alt.text.mutation

import dev.hbeck.alt.text.proto.UserActionEvent

interface UserActionHandler {
    fun handleEvent(event: UserActionEvent): Boolean
}