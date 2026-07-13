plugins {
    kotlin("jvm")
    id("io.github.baole.konture")
}

dependencies {
    // Standard test implementation of the unified Konture assertion library
    testImplementation("io.github.baole:konture:0.6.8")

    // JUnit 5 test dependencies
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}
