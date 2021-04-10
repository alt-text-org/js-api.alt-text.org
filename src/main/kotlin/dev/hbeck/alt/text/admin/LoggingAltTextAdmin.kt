package dev.hbeck.alt.text.admin

import dev.hbeck.alt.text.proto.UserImageRecord
import mu.KotlinLogging

val log = KotlinLogging.logger{}

class LoggingAltTextAdmin : AltTextAdmin {
    override fun report(
        imgHash: String,
        usernameHash: String,
        username: String,
        reason: String,
        imgRecord: UserImageRecord
    ) {
        log.error { "Got report: imgHash: '$imgHash' usernameHash: '$usernameHash' username: '$username' reason: '$reason': $imgRecord" }
    }
}