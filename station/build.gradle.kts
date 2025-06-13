plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
    kotlin("plugin.jpa")
    id("org.springframework.boot")
    application
    `java-library`
}

application {
    mainClass.set("io.orbital.station.OrbitalStationApp")
    applicationName = "orbital"
}

dependencies {
    implementation(project(":plugin-loader"))
    implementation("org.springframework.boot:spring-boot-starter-mustache")
    implementation(project(":schema-server-core"))
    implementation(project(":cockpit-core"))
    implementation(project(":copilot"))
    implementation("org.springframework.cloud:spring-cloud-starter-loadbalancer")
    implementation(project(":query-node-core"))
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation(project(":auth-common"))
    implementation(project(":avro-message-format"))
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation(project(":license-client"))
    testImplementation(project(":vyne-core-types")) {
        artifact {
            type = "test-jar"
        }
    }
    implementation(project(":hazelcast-connector"))
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("io.micrometer:micrometer-core")
    implementation("io.micrometer:micrometer-registry-jmx")
    implementation("io.micrometer:micrometer-registry-prometheus")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.session:spring-session-core")
    implementation("org.springframework.session:spring-session-hazelcast")
    implementation("org.springframework.security:spring-security-oauth2-client")
    implementation("org.springframework.security:spring-security-oauth2-jose")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
    implementation("org.postgresql:postgresql")
    implementation("net.logstash.logback:logstash-logback-encoder")
    implementation("org.zalando:logbook-spring-boot-starter")
    implementation("org.zalando:logbook-netty")
    implementation("org.zalando:logbook-json")
    implementation(project(":monitoring-common"))
    implementation(project(":vyne-spring-http"))
    implementation(project(":vyne-spring"))
    implementation(project(":vyne-analytics-server"))
    implementation(project(":analytics"))
    implementation(project(":history-service"))
    implementation(project(":s3-connector"))
    implementation(project(":sqs-connector"))
    implementation(project(":lambda-connector"))
    implementation(project(":servicebus-connector"))
    implementation(project(":blob-connector"))
    implementation(project(":kafka-connector"))
    implementation(project(":mongodb-connector"))
    implementation(project(":persistence-utils"))
    implementation(project(":metrics-utils"))

    // Vulnerability management
    implementation("org.apache.velocity:velocity-engine-core:2.4")
    implementation("org.eclipse.jgit:org.eclipse.jgit")
    implementation("com.google.protobuf:protobuf-java")
}

// Configure Spring Boot to not repackage (like Maven configuration)
tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    enabled = false
}

// Create startup scripts with JVM arguments
tasks.named<CreateStartScripts>("startScripts") {
    defaultJvmOpts = listOf(
        "--add-exports=java.base/jdk.internal.ref=ALL-UNNAMED",
        "--add-exports=java.base/sun.nio.ch=ALL-UNNAMED",
        "--add-exports=jdk.unsupported/sun.misc=ALL-UNNAMED",
        "--add-opens=java.base/java.lang=ALL-UNNAMED",
        "--add-opens=java.base/java.lang.reflect=ALL-UNNAMED",
        "--add-opens=java.base/java.io=ALL-UNNAMED",
        "--add-opens=java.base/java.util=ALL-UNNAMED"
    )
}

// Generate build-info for Spring Boot
springBoot {
    buildInfo {
        properties {
            additional.putAll(mapOf(
                "baseVersion" to "${project.version}".split("-")[0],
                "buildNumber" to (project.findProperty("buildNumber") ?: "0")
            ))
        }
    }
}

// Create custom distribution task to match Maven assembly
tasks.register<Zip>("createDistribution") {
    dependsOn("installDist")
    archiveFileName.set("orbital.zip")
    destinationDirectory.set(file("$buildDir/distributions"))
    
    from("$buildDir/install/${project.name}")
    
    // Include static resources if they exist
    from("src/main/resources/static") {
        into("static")
    }
}

tasks.named("assemble") {
    dependsOn("createDistribution")
}
