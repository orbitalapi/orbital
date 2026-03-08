plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    `java-library`
    `java-test-fixtures`
}

dependencies {
    // Taxi dependencies
    implementation("org.taxilang:lang-to-taxi-api")
    implementation("org.taxilang:taxi-stdlib-annotations")
    implementation("org.taxilang:core-types")
    implementation("org.taxilang:taxi-jvm-common")
    implementation("org.taxilang:taxi-annotations")
    
    // Kotlin Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-slf4j")
    
    // Kotlin Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-cbor")
    
    // Jackson
    implementation("com.fasterxml.jackson.core:jackson-annotations")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    
    // Other dependencies
    implementation("com.github.zafarkhaja:java-semver:0.9.0")
    implementation(project(":utils"))
    implementation("org.apache.commons:commons-csv:1.10.0")
    implementation("com.jayway.jsonpath:json-path:2.9.0")
    implementation("io.arrow-kt:arrow-core")
    
    // Test dependencies
    testImplementation("org.taxilang:compiler:1.65.0-SNAPSHOT") {
        artifact {
            classifier = "tests"
            type = "jar"
        }
    }
    testImplementation("com.nhaarman.mockitokotlin2:mockito-kotlin:2.2.0")
    testImplementation("net.datafaker:datafaker:2.2.2")
}
