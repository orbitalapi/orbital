plugins {
    kotlin("jvm")
    `java-library`
}

dependencies {
    // Hypersistence Utils
    implementation("io.hypersistence:hypersistence-utils-hibernate-63:3.7.3")
    
    // Hibernate
    implementation("org.hibernate:hibernate-core")
    
    // Spring
    implementation("org.springframework:spring-webflux")
    implementation("org.springframework.security:spring-security-oauth2-jose")
    implementation("org.springframework.security:spring-security-web")
    implementation("org.springframework.security:spring-security-oauth2-resource-server")
    
    // Internal dependencies
    implementation(project(":vyne-core-types"))
    
    // Jakarta Persistence
    implementation("jakarta.persistence:jakarta.persistence-api")
    
    // PAC4J
    implementation("org.pac4j:spring-security-pac4j")
    implementation("org.pac4j:pac4j-core")
    implementation("org.pac4j:pac4j-saml")
    
    // Bouncy Castle
    implementation("org.bouncycastle:bcprov-jdk18on")
    implementation("org.bouncycastle:bcpkix-jdk18on")
    
    // Logging (required for Kotlin logging)
    implementation("io.github.microutils:kotlin-logging-jvm")
    
    // HTTP4K (used in the source code)
    implementation("org.http4k:http4k-core")
    
    // HOCON config (used by VyneUserRoleDefinitionFileRepository)
    implementation("com.typesafe:config:1.4.3")
}
