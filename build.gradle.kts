import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.4.10"
    kotlin("kapt") version "1.4.31"
    application
}

group = "com.deepbeginnings"
version = "0.1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    api("org.graalvm.sdk:graal-sdk:21.0.0.2")
    api("org.graalvm.truffle:truffle-api:21.0.0.2")
    api("org.graalvm.truffle:truffle-dsl-processor:21.0.0.2")
    api("org.graalvm.truffle:truffle-tck:21.0.0.2")
    api("org.clojure:clojure:1.10.3")
    
    testImplementation(kotlin("test-junit"))
}

tasks.test {
    useJUnit()
}

tasks.withType<KotlinCompile>() {
    kotlinOptions.jvmTarget = "1.8"
}

application {
    mainClassName = "MainKt"
}
