plugins {
    kotlin("jvm")
    `java-library`
}

dependencies {
    // Core dependencies from utils/pom.xml
    api("org.eclipse.collections:eclipse-collections-api")
    implementation("org.eclipse.collections:eclipse-collections")
    implementation("com.google.guava:guava")
    implementation("io.projectreactor:reactor-core:3.5.3")
    implementation("io.arrow-kt:arrow-core")
    implementation("com.aventrix.jnanoid:jnanoid")
    
    // Missing dependencies for compilation
    implementation("commons-io:commons-io")
    implementation("org.apache.commons:commons-lang3")
    implementation("io.github.microutils:kotlin-logging-jvm")
    implementation("org.slf4j:slf4j-api")
    
    // Test dependencies
    testImplementation("io.projectreactor:reactor-test")
}