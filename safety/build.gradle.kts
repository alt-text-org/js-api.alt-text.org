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
    implementation("io.github.microutils:kotlin-logging:${Versions.kotlinLogging}")
    implementation("com.google.inject:guice:${Versions.guice}")

    implementation("org.alt-text:alt-text-protos:${Versions.altTextProtos}")
    implementation("com.github.streem.pbandk:pbandk-runtime-jvm:${Versions.pbandk}")
}
