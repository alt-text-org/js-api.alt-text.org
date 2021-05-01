package dev.hbeck.alt.text.http.resource

import com.google.inject.Inject
import com.google.inject.Singleton
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response


@Path("/healthcheck")
@Singleton
class HealthcheckResource @Inject constructor() {

    @GET
    fun healthcheck(): Response {
        return Response.ok().build()
    }
}