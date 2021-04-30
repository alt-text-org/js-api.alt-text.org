plugins {
    kotlin("jvm")
}

version = "unspecified"
val pbandkVersion by extra("0.8.1")

repositories {
    mavenCentral()
    mavenLocal()
    maven("https://jitpack.io")
    maven("https://maven.pkg.github.com/alt-text-org/alt-text-protos") {
        credentials(HttpHeaderCredentials::class) {
            name = "Authorization"
            value = "Bearer ${project.findProperty("gpr.token") as String}"
        }
        authentication {
            create<HttpHeaderAuthentication>("header")
        }
    }
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("io.github.microutils:kotlin-logging:1.6.22")
    implementation("javax.validation:validation-api:2.0.1.Final")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.11.0")

    implementation("com.google.inject:guice:4.2.0")
    implementation("com.google.firebase:firebase-admin:7.1.1")
    implementation("com.google.oauth-client:google-oauth-client:1.31.2")
    implementation("com.google.oauth-client:google-oauth-client-jetty:1.31.2")
    implementation("com.google.cloud:google-cloud-secretmanager:1.4.2")

    implementation("org.alt-text:alt-text-protos:0.6.4")
    implementation("com.github.streem.pbandk:pbandk-runtime-jvm:$pbandkVersion")
}
