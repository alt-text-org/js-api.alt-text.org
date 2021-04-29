package dev.hbeck.alt.text

import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.google.inject.Guice
import com.google.inject.Injector
import com.google.inject.Stage
import dev.hbeck.alt.text.http.auth.AuthModule
import dev.hbeck.alt.text.http.auth.BannedFilter
import dev.hbeck.alt.text.http.auth.MultiAuthFilter
import dev.hbeck.alt.text.http.resource.AdminAltTextResource
import dev.hbeck.alt.text.http.resource.HealthcheckResource
import dev.hbeck.alt.text.http.resource.PublicAltTextResource
import dev.hbeck.alt.text.http.resource.TwitterResource
import dev.hbeck.alt.text.storage.StorageModule
import dev.hbeck.alt.text.twitter.TwitterModule
import io.dropwizard.Application
import io.dropwizard.auth.AuthDynamicFeature
import io.dropwizard.setup.Bootstrap
import io.dropwizard.setup.Environment
import org.eclipse.jetty.servlets.CrossOriginFilter

import javax.servlet.DispatcherType

import java.util.EnumSet


class AltTextServer : Application<AltTextConfiguration>() {
    override fun initialize(bootstrap: Bootstrap<AltTextConfiguration>) {
        bootstrap.objectMapper?.registerModule(KotlinModule())
    }

    override fun getName(): String = "alt-text-library"

    override fun run(conf: AltTextConfiguration, env: Environment) {
        val injector = Guice.createInjector(
            Stage.PRODUCTION,
            ConfigModule(conf),
            StorageModule(conf.firestoreConfig),
            TwitterModule(),
            AuthModule(conf.authConfiguration)
        )!!

        configureAuth(env, injector)
        configureResources(env, injector)
        configureCors(env)
    }

    private fun configureCors(env: Environment) {
        val publicCors = env.servlets().addFilter("PublicCORS", CrossOriginFilter::class.java)
        publicCors.setInitParameter("allowedOrigins", "*")
        publicCors.setInitParameter("allowedHeaders", "X-Requested-With,Content-Type,Accept,Origin")
        publicCors.setInitParameter("allowedMethods", "OPTIONS,GET,POST,DELETE,HEAD")
        publicCors.addMappingForUrlPatterns(EnumSet.allOf(DispatcherType::class.java), true, "/api/alt-text/public/*", "/api/twitter/*")
        publicCors.setInitParameter(CrossOriginFilter.CHAIN_PREFLIGHT_PARAM, "false")

        val adminCors = env.servlets().addFilter("AdminCORS", CrossOriginFilter::class.java)
        adminCors.setInitParameter("allowedOrigins", "alt-text.org")
        adminCors.setInitParameter("allowedHeaders", "X-Requested-With,Content-Type,Accept,Origin")
        adminCors.setInitParameter("allowedMethods", "OPTIONS,GET,POST,DELETE,HEAD")
        adminCors.addMappingForUrlPatterns(EnumSet.allOf(DispatcherType::class.java), true, "/api/alt-text/admin/*")
        adminCors.setInitParameter(CrossOriginFilter.CHAIN_PREFLIGHT_PARAM, "false")
    }

    private fun configureResources(env: Environment, injector: Injector) {
        env.jersey().register(injector.getInstance(TwitterResource::class.java))
        env.jersey().register(injector.getInstance(PublicAltTextResource::class.java))
        env.jersey().register(injector.getInstance(AdminAltTextResource::class.java))
        env.jersey().register(injector.getInstance(HealthcheckResource::class.java))
    }

    private fun configureAuth(env: Environment, injector: Injector) {
        env.jersey().register(AuthDynamicFeature(injector.getInstance(MultiAuthFilter::class.java)))
        env.jersey().register(injector.getInstance(BannedFilter::class.java))
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            AltTextServer().run(*args)
        }
    }
}
