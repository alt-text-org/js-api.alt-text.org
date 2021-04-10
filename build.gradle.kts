import com.google.protobuf.gradle.*
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.4.32"
    kotlin("plugin.serialization") version "1.4.32"
    id("com.google.protobuf") version "0.8.12"
    id("com.google.cloud.tools.jib") version "2.5.0"
    application
}

val kotlinxSerializationVersion by extra("0.20.0")
val protobufVersion by extra("3.11.1")
val pbandkVersion by extra("0.9.1")

repositories {
    jcenter()
    mavenCentral()
    maven("https://jitpack.io")
    maven("http://jcenter.bintray.com")
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

    implementation("pro.streem.pbandk:pbandk-runtime:$pbandkVersion")
    implementation("io.lktk:blake3jni:0.2.2")
    implementation("com.github.seratch:signedrequest4j:2.14")

    implementation("com.google.inject:guice:4.2.0")
    implementation("com.google.firebase:firebase-admin:7.1.1")
    implementation("com.google.oauth-client:google-oauth-client:1.31.2")
    implementation("com.google.oauth-client:google-oauth-client-jetty:1.31.2")
    implementation("com.google.cloud:google-cloud-secretmanager:1.4.2")

    testImplementation("junit:junit:4.12")
    testImplementation("org.mockito:mockito-core:2.22.0")
    testImplementation("com.nhaarman.mockitokotlin2:mockito-kotlin:2.0.0")
    testImplementation("com.github.tdomzal:junit-docker-rule:0.4.1")
}

protobuf {
    generatedFilesBaseDir = "$projectDir/src"
    protoc {
        artifact = "com.google.protobuf:protoc:$protobufVersion"
    }
    plugins {
        id("kotlin") {
            artifact = "pro.streem.pbandk:protoc-gen-kotlin-jvm:$pbandkVersion:jvm8@jar"
        }
    }
    generateProtoTasks {
        ofSourceSet("main").forEach { task ->
            task.builtins {
                remove("java")
            }
            task.plugins {
                id("kotlin") {
                    option("kotlin_package=dev.hbeck.alt.text.proto")
                }
            }
        }
    }
}

configurations.named("compileProtoPath") {
    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_API))
        attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.LIBRARY))
    }
}

configurations.named("testCompileProtoPath") {
    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_API))
        attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.LIBRARY))
    }
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
