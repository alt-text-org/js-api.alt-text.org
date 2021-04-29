package dev.hbeck.alt.text.http.auth.principal

import java.security.Principal


sealed class UserPrincipal(val roles: Set<String>) : Principal

class GooglePrincipal(roles: Set<String>, val userId: String, val email: String?) : UserPrincipal(roles) {
    override fun getName(): String {
        return "google:$userId"
    }
}

class TwitterPrincipal(roles: Set<String>, val userId: String, val screenName: String?, ) : UserPrincipal(roles) {
    override fun getName(): String {
        return "twitter:$userId"
    }
}

class EmailPrincipal(roles: Set<String>, val email: String) : UserPrincipal(roles) {
    override fun getName(): String {
        return "email:$email"
    }
}
