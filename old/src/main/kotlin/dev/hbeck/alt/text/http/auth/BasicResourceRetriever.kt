package dev.hbeck.alt.text.http.auth

import com.google.inject.Inject
import com.nimbusds.jose.util.Resource
import com.nimbusds.jose.util.ResourceRetriever
import java.net.URL
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration


class BasicResourceRetriever @Inject constructor(
    private val client: HttpClient,
    configuration: AuthConfiguration
) : ResourceRetriever {

    private val timeout = Duration.ofSeconds(configuration.resourceRetrieverTimeoutSeconds)
    private val json = "application/json"

    override fun retrieveResource(url: URL): Resource {
        val request = HttpRequest.newBuilder(url.toURI())
            .header("Accept", json)
            .version(HttpClient.Version.HTTP_1_1)
            .timeout(timeout)
            .GET()
            .build()

        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        val contentType = response.headers().firstValue("Content-Type")
        if (contentType.orElse("") != json) {
            throw RuntimeException("Got unexpected or missing Content-Type: $contentType")
        }

        return Resource(response.body(), json)
    }
}