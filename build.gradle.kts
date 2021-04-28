import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.4.32"
    kotlin("plugin.serialization") version "1.4.32"
    id("com.google.cloud.tools.jib") version "2.5.0"
    application
}

val kotlinxSerializationVersion by extra("0.20.0")
val pbandkVersion by extra("0.8.1")

repositories {
    maven("http://jcenter.bintray.com") {
        isAllowInsecureProtocol = true
    }
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

application {
    mainClassName = "dev.hbeck.alt.text.AltTextServer"
    applicationName = "AltTextServer"
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime:$kotlinxSerializationVersion")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.3.72")
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:1.3.72")
    implementation("io.github.microutils:kotlin-logging:1.6.22")

    implementation("io.dropwizard:dropwizard-core:2.0.20")
    implementation("io.dropwizard:dropwizard-auth:2.0.20")
    implementation("com.nimbusds:oauth2-oidc-sdk:9.3")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.11.0")
    implementation("org.glassfish.jersey.bundles.repackaged:jersey-guava:2.6")

    implementation("io.lktk:blake3jni:0.2.2")
    implementation("com.github.seratch:signedrequest4j:2.14")

    implementation("com.google.inject:guice:4.2.0")
    implementation("com.google.firebase:firebase-admin:7.1.1")
    implementation("com.google.oauth-client:google-oauth-client:1.31.2")
    implementation("com.google.oauth-client:google-oauth-client-jetty:1.31.2")
    implementation("com.google.cloud:google-cloud-secretmanager:1.4.2")

    implementation("org.alt-text:alt-text-protos:0.6.0")
    implementation("com.github.streem.pbandk:pbandk-runtime-jvm:$pbandkVersion")

    /* Pinecone Deps */
    implementation("io.netty:netty-tcnative-boringssl-static:2.0.35.Final")
    implementation("io.grpc:grpc-protobuf:1.35.0")
    implementation("io.grpc:grpc-stub:1.35.0")
    implementation("io.grpc:grpc-netty:1.35.0")
    implementation("org.slf4j:slf4j-api:1.7.30")


    testImplementation("junit:junit:4.12")
    testImplementation("org.mockito:mockito-core:2.22.0")
    testImplementation("com.nhaarman.mockitokotlin2:mockito-kotlin:2.0.0")
    testImplementation("com.github.tdomzal:junit-docker-rule:0.4.1")
}

tasks {
    compileJava {
        enabled = false
    }

    withType<KotlinCompile>().configureEach {
        kotlinOptions {
            freeCompilerArgs = freeCompilerArgs + "-Xopt-in=kotlin.RequiresOptIn"
            jvmTarget = "11"
        }
    }
}
