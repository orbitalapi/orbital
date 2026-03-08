plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    `java-library`
    `java-test-fixtures`
}

dependencies {
    // Test dependencies
    testImplementation("app.cash.turbine:turbine-jvm")
    testImplementation("com.google.guava:guava-testlib")
    testImplementation("io.projectreactor:reactor-test")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test")
    testImplementation("com.squareup.okhttp3:okhttp")
    testImplementation("com.squareup.okhttp3:mockwebserver")
    testImplementation("org.skyscreamer:jsonassert:1.5.1")
    testImplementation(testFixtures(project(":vyne-core-types")))
    testImplementation("org.taxilang:compiler:1.65.0-SNAPSHOT") {
        artifact {
            classifier = "tests"
            type = "jar"
        }
    }
    testImplementation("com.orbitalhq:vyne-spring-http:0.36.0-SNAPSHOT")
    
    // Test fixtures dependencies (shared by other modules)
    testFixturesImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
    testFixturesImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:1.9.0")
    
    // Main dependencies
    implementation(project(":auth-common"))
    implementation(project(":datatype-converters"))
    implementation(project(":vyne-core-types"))
    implementation(project(":vyne-query-api"))
    implementation(project(":persistence-utils"))
    implementation(project(":vyne-csv-utils"))
    implementation(project(":utils"))
    
    // External dependencies
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.1")
    implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")
    implementation("org.taxilang:taxi-jvm-common:1.65.0-SNAPSHOT")
    implementation("jakarta.persistence:jakarta.persistence-api")
    implementation("org.taxilang:compiler:1.65.0-SNAPSHOT")
    implementation("org.taxilang:taxi-annotations:1.65.0-SNAPSHOT")
    implementation("org.apache.commons:commons-csv:1.10.0")
    implementation("es.usc.citius.hipster:hipster-core:1.0.1") {
        exclude(group = "com.google.guava", module = "guava")
    }
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-slf4j:1.9.0")
    implementation("io.micrometer:micrometer-core")
    implementation("com.spikhalskiy.futurity:futurity-core:0.3-RC3")
}
