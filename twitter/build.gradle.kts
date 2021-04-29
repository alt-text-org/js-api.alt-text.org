plugins {
    kotlin("jvm")
}

version = "unspecified"

val kotlinxSerializationVersion by extra("0.20.0")

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
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime:$kotlinxSerializationVersion")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.3.72")
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:1.3.72")
    implementation("io.github.microutils:kotlin-logging:1.6.22")
    implementation("javax.validation:validation-api:2.0.1.Final")
    implementation("javax.ws.rs:javax.ws.rs-api:2.1.1")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.11.0")

    implementation("org.alt-text:alt-text-protos:0.6.1")

    implementation("com.google.inject:guice:4.2.0")
    implementation("com.google.guava:guava:30.1.1-jre")
    implementation("com.google.cloud:google-cloud-secretmanager:1.4.2")

    implementation("com.github.seratch:signedrequest4j:2.14")
}
