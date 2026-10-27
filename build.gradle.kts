plugins {
    // 1. Core plugin to add compilation and testing support for Java
    java
    // 2. Optional: Adds enhanced capabilities specifically for publishing reusable libraries
    `java-library`
}

group = "io.github.sifisofakude.sdkmanager"
version = "1.0.0"

repositories {
    // Looks for external dependencies in the central Maven repository
    mavenCentral()
}

dependencies {
    // Use 'api' for dependencies that form part of your library's public API visible to consumers
    // api("org.apache.commons:commons-lang3:3.12.0")

    implementation("io.github.sifisofakude.filesystem:filesystem:1.0.0")
    implementation("io.github.sifisofakude.serverresults:serverresults:3.0.0")
    implementation("org.jetbrains.kotlinx:kotlinx-io-core:0.9.1")

    // Use 'implementation' for internal dependencies hidden from consumers
    // implementation("com.google.gson:gson:2.10.1")

    // Setup JUnit 5 for running your unit tests
    testImplementation(platform("org.junit:junit-bom:5.10.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

java {
    // Compiles code and generates bytecode target for Java 17
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
    
    // Automatically packages source code files into a separate '-sources.jar' alongside your library
    withSourcesJar()
    
    // Automatically packages Javadoc documentation into a '-javadoc.jar'
    withJavadocJar()
}

tasks.test {
    // Instructs Gradle to use the JUnit Platform engine to discover and run tests
    useJUnitPlatform()
}
