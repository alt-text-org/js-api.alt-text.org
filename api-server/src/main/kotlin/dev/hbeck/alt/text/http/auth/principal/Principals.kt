package dev.hbeck.alt.text.http.auth.principal

import dev.hbeck.alt.text.proto.SubmittingUserInfo
import java.security.Principal


sealed class UserPrincipal(val roles: Set<String>) : Principal {
    abstract fun toSubmittingUser(): SubmittingUserInfo
}

class GooglePrincipal(roles: Set<String>, val userId: String, val email: String?) : UserPrincipal(roles) {
    override fun toSubmittingUser(): SubmittingUserInfo = SubmittingUserInfo(
        username = userId,
        provider = "google"
    )

    override fun getName(): String {
        return "google:$userId"
    }
}

class TwitterPrincipal(roles: Set<String>, val userId: String, val screenName: String?, ) : UserPrincipal(roles) {
    override fun toSubmittingUser(): SubmittingUserInfo = SubmittingUserInfo(
        username = userId,
        provider = "twitter"
    )

    override fun getName(): String {
        return "twitter:$userId"
    }
}

class EmailPrincipal(roles: Set<String>, val email: String) : UserPrincipal(roles) {
    override fun toSubmittingUser(): SubmittingUserInfo = SubmittingUserInfo(
        username = email,
        provider = "email"
    )

    override fun getName(): String {
        return "email:$email"
    }
}
