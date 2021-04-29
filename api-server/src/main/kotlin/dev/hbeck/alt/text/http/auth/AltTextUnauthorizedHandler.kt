package dev.hbeck.alt.text.http.auth

import io.dropwizard.auth.UnauthorizedHandler
import javax.ws.rs.core.HttpHeaders
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response


class AltTextUnauthorizedHandler : UnauthorizedHandler {
    override fun buildResponse(prefix: String, realm: String): Response {
        return Response.status(Response.Status.UNAUTHORIZED)
            .header(HttpHeaders.WWW_AUTHENTICATE, """$prefix realm="$realm"""")
            .type(MediaType.APPLICATION_JSON_TYPE)
            .entity("""{"message": "You must be authorized to access this endpoint"}""")
            .build()
    }
}