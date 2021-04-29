plugins {
    java
    kotlin("jvm")
}

version = "unspecified"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))

    implementation("io.netty:netty-tcnative-boringssl-static:2.0.35.Final")
    implementation("io.grpc:grpc-protobuf:1.35.0")
    implementation("io.grpc:grpc-stub:1.35.0")
    implementation("io.grpc:grpc-netty:1.35.0")
    implementation("org.slf4j:slf4j-api:1.7.30")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.6.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}