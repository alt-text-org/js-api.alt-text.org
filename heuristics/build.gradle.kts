import dev.hbeck.alt.text.shared.Versions

plugins {
    java
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

    implementation(project(":common"))

    implementation("com.google.inject:guice:${Versions.guice}")

    implementation("io.netty:netty-tcnative-boringssl-static:${Versions.nettyTcnativeBoringSSL}")
    implementation("io.grpc:grpc-protobuf:${Versions.grpc}")
    implementation("io.grpc:grpc-stub:${Versions.grpc}")
    implementation("io.grpc:grpc-netty:${Versions.grpc}")
    implementation("org.slf4j:slf4j-api:${Versions.slf4j}")

    implementation("org.alt-text:alt-text-protos:${Versions.altTextProtos}")
    implementation("com.github.streem.pbandk:pbandk-runtime-jvm:${Versions.pbandk}")

    testImplementation("org.junit.jupiter:junit-jupiter-api:${Versions.jupiter}")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:${Versions.jupiter}")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}