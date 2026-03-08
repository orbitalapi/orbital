plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
    id("org.springframework.boot")
    id("org.graalvm.buildtools.native")
    `java-library`
}

dependencies {
    testImplementation("io.projectreactor:reactor-test")
    testImplementation("org.testcontainers:junit-jupiter")
    implementation("io.projectreactor:reactor-tools")
    implementation("io.projectreactor.rabbitmq:reactor-rabbitmq:1.5.6")
    testImplementation("org.testcontainers:rabbitmq")
    
    // Spring Cloud Function
    implementation("org.springframework.cloud:spring-cloud-function-context")
    implementation("org.springframework.cloud:spring-cloud-function-web")
    implementation("org.springframework.cloud:spring-cloud-function-adapter-aws")
    
    // AWS Lambda
    implementation("com.amazonaws:aws-lambda-java-events:3.9.0")
    compileOnly("com.amazonaws:aws-lambda-java-core:1.1.0")
    
    // Spring Boot
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    
    // Jackson and Kotlin
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
    
    // Internal dependencies
    implementation(project(":monitoring-common"))
    implementation(project(":query-node-api"))
    implementation(project(":vyne-spring")) {
        exclude(group = "commons-logging", module = "commons-logging")
    }
    implementation(project(":query-node-core")) {
        exclude(group = "commons-logging", module = "commons-logging")
        exclude(group = "com.fasterxml.woodstox", module = "woodstox-core")
    }
    implementation(project(":vyne-history-core"))
    implementation(project(":jdbc-connector"))
    implementation(project(":dynamo-db-connector"))
    implementation(project(":vyne-core-types"))
    
    // Jackson
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
    
    // Test dependencies
    testImplementation("io.kotest:kotest-runner-junit5-jvm")
    testImplementation("io.kotest:kotest-assertions-core-jvm")
}

// Exclude woodstox from all configurations to prevent native compilation issues
configurations.all {
    exclude(group = "com.fasterxml.woodstox", module = "woodstox-core")
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.cloud:spring-cloud-function-dependencies:4.0.2")
    }
}

graalvmNative {
    binaries {
        named("main") {
            buildArgs.add("--initialize-at-build-time=org.apache.commons.logging,com.ctc.wstx.api.ReaderConfig")
            verbose.set(true)
        }
    }
    metadataRepository {
        enabled.set(true)
        version.set("0.3.14")
    }
}

tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    archiveFileName.set("${project.name}.jar")
}
