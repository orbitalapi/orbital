import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.springframework.boot") version "3.3.11" apply false
    id("io.spring.dependency-management") version "1.1.6" apply false
    kotlin("jvm") version "1.9.24" apply false
    kotlin("plugin.spring") version "1.9.24" apply false
    kotlin("plugin.jpa") version "1.9.24" apply false
    kotlin("plugin.serialization") version "1.9.24" apply false
    id("org.graalvm.buildtools.native") version "0.10.3" apply false
    id("com.github.jk1.dependency-license-report") version "2.8" apply false
    id("com.github.hierynomus.license") version "0.16.1" apply false
    id("net.researchgate.release") version "3.0.2" apply false
}

group = "com.orbitalhq"
version = "0.36.0-SNAPSHOT"

// Centralized version management
val versions = mapOf(
    "kotlin" to "1.9.24",
    "kotlinxCoroutines" to "1.9.0", 
    "kotlinxSerialization" to "1.5.1",
    "kotlinDataframe" to "0.14.0",
    "taxi" to "1.65.0-SNAPSHOT",
    "elasticsearch" to "7.5.2",
    "kotlinLogging" to "3.0.5",
    "springCloud" to "2023.0.1",
    "arrow" to "1.1.5",
    "logstashLogback" to "7.4",
    "http4k" to "4.48.0.0",
    "hazelcast" to "5.4.0",
    "testcontainers" to "1.19.3",
    "guava" to "32.1.3-jre",
    "reactorBom" to "2023.0.0",
    "logbook" to "3.12.0",
    "jgit" to "6.9.0.202403050737-r",
    "kotest" to "5.6.2",
    "jsonPath" to "2.9.0",
    "awsS3Sdk" to "1.12.778",
    "awsDynamoSdk" to "1.12.701",
    "netty" to "4.1.118.Final", // CVE-2025-24970 can probably remove after spring boot > 3.4.2
    "protobuf" to "3.25.5", // CVE-2024-7254 - Transitive dependency in wire-compiler and calcite-core
    "commonsIo" to "2.17.0", // CVE-2024-47554 in <2.14.0
    "bouncycastle" to "1.78",
    "eclipseCollections" to "11.1.0"
)

// Task to print version for CI
tasks.register("printVersion") {
    doLast {
        println(project.version)
    }
}

// Apply license reporting to root project  
apply(plugin = "com.github.jk1.dependency-license-report")

configure<com.github.jk1.license.LicenseReportExtension> {
    outputDir = "$buildDir/reports/dependency-license"
    projects = arrayOf(project)
    configurations = arrayOf("compileClasspath")
}

allprojects {
    repositories {
        mavenCentral()
        maven {
            name = "OrbitalSnapshots"
            url = uri("https://repo.orbitalhq.com/snapshot")
            content {
                includeGroup("com.orbitalhq")
                includeGroup("org.taxilang")
            }
        }
        maven {
            name = "OrbitalRelease"
            url = uri("https://repo.orbitalhq.com/release")
            content {
                includeGroup("com.orbitalhq")
            }
        }
        maven {
            name = "JooqPro"
            url = uri("https://repo.jooq.org/repo")
            credentials {
                username = System.getenv("JOOQ_REPO_USERNAME")
                password = System.getenv("JOOQ_REPO_PASSWORD")
            }
            content {
                includeGroup("org.jooq.pro")
            }
        }
    }
}

subprojects {
    apply(plugin = "kotlin")
    apply(plugin = "io.spring.dependency-management")
    apply(plugin = "maven-publish")

    configure<JavaPluginExtension> {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    configure<PublishingExtension> {
        repositories {
            maven {
                name = "orbital"
                url = uri(if (version.toString().contains("SNAPSHOT")) {
                    "s3://repo.orbitalhq.com/snapshot"
                } else {
                    "s3://repo.orbitalhq.com/release"
                })
            }
        }
    }

    tasks.withType<KotlinCompile> {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
            freeCompilerArgs.add("-Xjsr305=strict")
        }
    }

    tasks.withType<Test> {
        useJUnitPlatform()
        jvmArgs(
            "--add-exports=java.base/jdk.internal.ref=ALL-UNNAMED",
            "--add-exports=java.base/sun.nio.ch=ALL-UNNAMED", 
            "--add-exports=jdk.unsupported/sun.misc=ALL-UNNAMED",
            "--add-exports=jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED",
            "--add-opens=jdk.compiler/com.sun.tools.javac=ALL-UNNAMED",
            "--add-opens=java.base/java.lang=ALL-UNNAMED",
            "--add-opens=java.base/java.lang.reflect=ALL-UNNAMED",
            "--add-opens=java.base/java.io=ALL-UNNAMED",
            "--add-opens=java.base/java.util=ALL-UNNAMED"
        )
        systemProperty("surefire.rerunFailingTestsCount", "2")
        systemProperty("env.buildServer", "true")
    }

    the<io.spring.gradle.dependencymanagement.dsl.DependencyManagementExtension>().apply {
        imports {
            mavenBom("org.springframework.boot:spring-boot-dependencies:3.3.11")
            mavenBom("org.springframework.cloud:spring-cloud-dependencies:${versions["springCloud"]}")
            mavenBom("org.zalando:logbook-bom:${versions["logbook"]}")
            mavenBom("software.amazon.awssdk:bom:2.29.14")
            mavenBom("io.projectreactor:reactor-bom:${versions["reactorBom"]}")
            mavenBom("org.http4k:http4k-bom:${versions["http4k"]}")
            mavenBom("com.fasterxml.jackson:jackson-bom:2.17.2")
            mavenBom("io.micrometer:micrometer-bom:1.13.1")
            mavenBom("org.testcontainers:testcontainers-bom:${versions["testcontainers"]}")
            mavenBom("net.openhft:chronicle-bom:2.25ea58")
        }
        
        dependencies {
            // Kotlin
            dependency("org.jetbrains.kotlin:kotlin-stdlib-jdk8:${versions["kotlin"]}")
            dependency("org.jetbrains.kotlin:kotlin-reflect:${versions["kotlin"]}")
            dependency("org.jetbrains.kotlin:kotlin-script-runtime:${versions["kotlin"]}")
            dependency("org.jetbrains.kotlin:kotlin-test-junit:${versions["kotlin"]}")
            
            // Kotlin Coroutines
            dependency("org.jetbrains.kotlinx:kotlinx-coroutines-core:${versions["kotlinxCoroutines"]}")
            dependency("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:${versions["kotlinxCoroutines"]}")
            dependency("org.jetbrains.kotlinx:kotlinx-coroutines-test:${versions["kotlinxCoroutines"]}")
            dependency("org.jetbrains.kotlinx:kotlinx-coroutines-reactive:${versions["kotlinxCoroutines"]}")
            
            // Kotlin Serialization
            dependency("org.jetbrains.kotlinx:kotlinx-serialization-core:${versions["kotlinxSerialization"]}")
            dependency("org.jetbrains.kotlinx:kotlinx-serialization-json:${versions["kotlinxSerialization"]}")
            dependency("org.jetbrains.kotlinx:kotlinx-serialization-hocon:${versions["kotlinxSerialization"]}")
            dependency("org.jetbrains.kotlinx:kotlinx-serialization-cbor:${versions["kotlinxSerialization"]}")
            
            // Taxi (requires snapshot repository access - see firewall configuration)
            dependency("org.taxilang:compiler:${versions["taxi"]}")
            dependency("org.taxilang:core-types:${versions["taxi"]}")
            dependency("org.taxilang:taxi-annotations:${versions["taxi"]}")
            dependency("org.taxilang:java2taxi:${versions["taxi"]}")
            dependency("org.taxilang:lang-to-taxi-api:${versions["taxi"]}")
            dependency("org.taxilang:taxi-stdlib-annotations:${versions["taxi"]}")
            dependency("org.taxilang:taxi-jvm-common:${versions["taxi"]}")
            
            // Logging
            dependency("io.github.microutils:kotlin-logging-jvm:${versions["kotlinLogging"]}")
            dependency("net.logstash.logback:logstash-logback-encoder:${versions["logstashLogback"]}")
            
            // Common utilities
            dependency("com.google.guava:guava:${versions["guava"]}")
            dependency("com.google.guava:guava-testlib:${versions["guava"]}")
            dependency("org.apache.commons:commons-lang3:3.12.0")
            dependency("commons-io:commons-io:${versions["commonsIo"]}") // CVE-2024-47554 in <2.14.0
            dependency("com.diffplug.durian:durian:3.4.0")
            
            // Eclipse Collections
            dependency("org.eclipse.collections:eclipse-collections-api:${versions["eclipseCollections"]}")
            dependency("org.eclipse.collections:eclipse-collections:${versions["eclipseCollections"]}")
            
            // Arrow
            dependency("io.arrow-kt:arrow-core:${versions["arrow"]}")
            
            // Other
            dependency("com.aventrix.jnanoid:jnanoid:2.0.0")
            dependency("app.cash.turbine:turbine-jvm:0.12.1")
            dependency("com.hazelcast:hazelcast:${versions["hazelcast"]}")
            dependency("org.eclipse.jgit:org.eclipse.jgit:${versions["jgit"]}")
            
            // Security vulnerabilities fixes
            dependency("org.apache.commons:commons-compress:1.26.0")
            dependency("com.google.protobuf:protobuf-java:${versions["protobuf"]}") // CVE-2024-7254 - Transitive dependency in wire-compiler and calcite-core
            dependency("com.rabbitmq:amqp-client:5.18.0")
            dependency("com.github.jsqlparser:jsqlparser:4.9")
            dependency("net.minidev:json-smart:2.5.2")
            dependency("org.jooq.pro:jooq:3.19.4")
            dependency("io.netty:netty-all:${versions["netty"]}") // CVE-2025-24970 can probably remove after spring boot > 3.4.2
            dependency("org.bouncycastle:bcprov-jdk18on:${versions["bouncycastle"]}")
            dependency("org.bouncycastle:bcpkix-jdk18on:${versions["bouncycastle"]}")
            
            // PAC4J
            dependency("org.pac4j:pac4j-core:6.0.6")
            dependency("org.pac4j:spring-security-pac4j:10.0.0")
            dependency("org.pac4j:pac4j-saml:6.0.6")
            
            // Test dependencies
            dependency("junit:junit:4.13.2")
            dependency("org.hamcrest:hamcrest-all:1.3")
            dependency("com.jayway.awaitility:awaitility:1.7.0")
            dependency("org.jetbrains.spek:spek-api:1.1.5")
            dependency("com.winterbe:expekt:0.5.0")
            dependency("com.nhaarman:mockito-kotlin:1.6.0")
            dependency("io.kotest:kotest-runner-junit5-jvm:${versions["kotest"]}")
            dependency("io.kotest:kotest-assertions-core-jvm:${versions["kotest"]}")
            
            // AWS
            dependency("com.amazonaws:aws-java-sdk-s3:${versions["awsS3Sdk"]}")
            dependency("com.amazonaws:aws-java-sdk-dynamodb:${versions["awsDynamoSdk"]}")
        }
    }
}
