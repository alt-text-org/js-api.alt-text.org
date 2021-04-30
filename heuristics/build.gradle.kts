plugins {
    java
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

    project(":common")

    implementation("com.google.inject:guice:4.2.0")

    implementation("io.netty:netty-tcnative-boringssl-static:2.0.35.Final")
    implementation("io.grpc:grpc-protobuf:1.35.0")
    implementation("io.grpc:grpc-stub:1.35.0")
    implementation("io.grpc:grpc-netty:1.35.0")
    implementation("org.slf4j:slf4j-api:1.7.30")

    implementation("org.alt-text:alt-text-protos:0.6.4")
    implementation("com.github.streem.pbandk:pbandk-runtime-jvm:$pbandkVersion")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.6.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}