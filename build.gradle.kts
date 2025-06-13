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
            }
        }
        maven {
            name = "TaxiSnapshots"
            url = uri("https://s01.oss.sonatype.org/content/repositories/snapshots")
            content {
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
            mavenBom("org.springframework.cloud:spring-cloud-dependencies:2023.0.1")
            mavenBom("org.zalando:logbook-bom:3.12.0")
            mavenBom("software.amazon.awssdk:bom:2.29.14")
            mavenBom("io.projectreactor:reactor-bom:2023.0.0")
            mavenBom("org.http4k:http4k-bom:4.48.0.0")
            mavenBom("com.fasterxml.jackson:jackson-bom:2.17.2")
            mavenBom("io.micrometer:micrometer-bom:1.13.1")
            mavenBom("org.testcontainers:testcontainers-bom:1.19.3")
            mavenBom("net.openhft:chronicle-bom:2.25ea58")
        }
        
        dependencies {
            // Kotlin
            dependency("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.9.24")
            dependency("org.jetbrains.kotlin:kotlin-reflect:1.9.24")
            dependency("org.jetbrains.kotlin:kotlin-script-runtime:1.9.24")
            dependency("org.jetbrains.kotlin:kotlin-test-junit:1.9.24")
            
            // Kotlin Coroutines
            dependency("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
            dependency("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:1.9.0")
            dependency("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
            dependency("org.jetbrains.kotlinx:kotlinx-coroutines-reactive:1.9.0")
            
            // Kotlin Serialization
            dependency("org.jetbrains.kotlinx:kotlinx-serialization-core:1.5.1")
            dependency("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.1")
            dependency("org.jetbrains.kotlinx:kotlinx-serialization-hocon:1.5.1")
            dependency("org.jetbrains.kotlinx:kotlinx-serialization-cbor:1.5.1")
            
            // Taxi
            dependency("org.taxilang:compiler:1.65.0-SNAPSHOT")
            dependency("org.taxilang:core-types:1.65.0-SNAPSHOT")
            dependency("org.taxilang:taxi-annotations:1.65.0-SNAPSHOT")
            dependency("org.taxilang:java2taxi:1.65.0-SNAPSHOT")
            dependency("org.taxilang:lang-to-taxi-api:1.65.0-SNAPSHOT")
            dependency("org.taxilang:taxi-stdlib-annotations:1.65.0-SNAPSHOT")
            dependency("org.taxilang:taxi-jvm-common:1.65.0-SNAPSHOT")
            
            // Logging
            dependency("io.github.microutils:kotlin-logging-jvm:3.0.5")
            dependency("net.logstash.logback:logstash-logback-encoder:7.4")
            
            // Common utilities
            dependency("com.google.guava:guava:32.1.3-jre")
            dependency("com.google.guava:guava-testlib:32.1.3-jre")
            dependency("org.apache.commons:commons-lang3:3.12.0")
            dependency("commons-io:commons-io:2.17.0")
            dependency("com.diffplug.durian:durian:3.4.0")
            
            // Eclipse Collections
            dependency("org.eclipse.collections:eclipse-collections-api:11.1.0")
            dependency("org.eclipse.collections:eclipse-collections:11.1.0")
            
            // Arrow
            dependency("io.arrow-kt:arrow-core:1.1.5")
            
            // Other
            dependency("com.aventrix.jnanoid:jnanoid:2.0.0")
            dependency("app.cash.turbine:turbine-jvm:0.12.1")
            dependency("com.hazelcast:hazelcast:5.4.0")
            dependency("org.eclipse.jgit:org.eclipse.jgit:6.9.0.202403050737-r")
            
            // Security vulnerabilities fixes
            dependency("org.apache.commons:commons-compress:1.26.0")
            dependency("com.google.protobuf:protobuf-java:3.25.5")
            dependency("com.rabbitmq:amqp-client:5.18.0")
            dependency("com.github.jsqlparser:jsqlparser:4.9")
            dependency("net.minidev:json-smart:2.5.2")
            dependency("org.jooq.pro:jooq:3.19.4")
            dependency("io.netty:netty-all:4.1.118.Final")
            dependency("org.bouncycastle:bcprov-jdk18on:1.78")
            dependency("org.bouncycastle:bcpkix-jdk18on:1.78")
            
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
            dependency("io.kotest:kotest-runner-junit5-jvm:5.6.2")
            dependency("io.kotest:kotest-assertions-core-jvm:5.6.2")
            
            // AWS
            dependency("com.amazonaws:aws-java-sdk-s3:1.12.778")
            dependency("com.amazonaws:aws-java-sdk-dynamodb:1.12.701")
        }
    }
}
