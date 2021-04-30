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

    implementation("org.alt-text:alt-text-protos:0.6.4")
    implementation("com.github.streem.pbandk:pbandk-runtime-jvm:$pbandkVersion")
}
