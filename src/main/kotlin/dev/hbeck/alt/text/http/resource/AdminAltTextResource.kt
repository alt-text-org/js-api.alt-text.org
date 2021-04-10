package dev.hbeck.alt.text.http.resource;

import dev.hbeck.alt.text.admin.AltTextAdmin
import javax.annotation.security.RolesAllowed
import javax.inject.Inject
import javax.inject.Singleton
import javax.ws.rs.*

import javax.ws.rs.core.MediaType

@Singleton
@Path("/api/alt-text/admin/v1")
@RolesAllowed("ADMIN")
class AdminAltTextResource @Inject constructor(private val admin: AltTextAdmin) {

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("")
    fun serve() {
    }

    @GET
    @Path("")
    fun get() {
    }
}
