import dev.hbeck.alt.text.shared.Versions

plugins {
    id("org.jetbrains.kotlin.jvm") version "1.5.0"
}

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
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime:${Versions.kotlinxSerialization}")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:${Versions.kotlinStdlib}")
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:${Versions.kotlinStdlib}")
    implementation("io.github.microutils:kotlin-logging:${Versions.kotlinLogging}")
    implementation("javax.validation:validation-api:${Versions.javaxValidation}")
    implementation("javax.ws.rs:javax.ws.rs-api:${Versions.javaxWsRs}")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:${Versions.jacksonModule}")

    implementation("org.alt-text:alt-text-protos:${Versions.altTextProtos}")
    implementation("com.github.streem.pbandk:pbandk-runtime-jvm:${Versions.pbandk}")

    implementation("com.google.inject:guice:${Versions.guice}")
    implementation("com.google.guava:guava:${Versions.guava}")
    implementation("com.google.cloud:google-cloud-secretmanager:${Versions.googleCloudSecretManager}")

    implementation("com.github.seratch:signedrequest4j:${Versions.signedRequest4j}")
}
