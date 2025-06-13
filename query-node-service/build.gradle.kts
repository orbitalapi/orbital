plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
    id("org.springframework.boot")
    `java-library`
}

dependencies {
    implementation(project(":soap-connector"))
    implementation(project(":vyne-history-core"))
    implementation("com.github.ben-manes.caffeine:caffeine")
    implementation(project(":query-node-core"))
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation(project(":vyne-spring"))
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("io.micrometer:micrometer-registry-jmx")
}

tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    archiveFileName.set("${project.name}.jar")
}

// Generate build-info
springBoot {
    buildInfo()
}
