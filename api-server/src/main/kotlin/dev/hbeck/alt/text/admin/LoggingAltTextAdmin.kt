package dev.hbeck.alt.text.admin

import mu.KotlinLogging

val log = KotlinLogging.logger{}

class LoggingAltTextAdmin : AltTextAdmin {
    override fun report(
        imgHash: String,
        usernameHash: String,
        username: String,
        reason: String
    ) {
        log.error { "Got report: imgHash: '$imgHash' usernameHash: '$usernameHash' username: '$username' reason: '$reason'" }
    }
}