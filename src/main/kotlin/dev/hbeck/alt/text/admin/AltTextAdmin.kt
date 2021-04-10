package dev.hbeck.alt.text.admin

import dev.hbeck.alt.text.proto.UserImageRecord


interface AltTextAdmin {
    fun report(imgHash: String, usernameHash: String, username: String, reason: String, imgRecord: UserImageRecord)
}