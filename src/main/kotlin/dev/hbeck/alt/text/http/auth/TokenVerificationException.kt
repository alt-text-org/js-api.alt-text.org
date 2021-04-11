package dev.hbeck.alt.text.http.auth

import java.lang.RuntimeException


class TokenVerificationException(message: String) : RuntimeException(message)