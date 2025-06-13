plugins {
    kotlin("jvm")
    `java-library`
}

dependencies {
    // Internal dependencies
    implementation(project(":vyne-core-types"))
    
    // Stormpot for object pooling  
    implementation("com.github.chrisvest:stormpot:3.1")
    
    // Spring Core (provided/optional in Maven)
    compileOnly("org.springframework:spring-core")
    
    // Logging 
    implementation("io.github.microutils:kotlin-logging-jvm")
    
    // Apache Commons Lang (for NumberUtils)
    implementation("org.apache.commons:commons-lang3")
}
