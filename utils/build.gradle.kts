plugins {
    kotlin("jvm")
    `java-library`
}

dependencies {
    api("org.eclipse.collections:eclipse-collections-api")
    implementation("org.eclipse.collections:eclipse-collections")
    implementation("com.google.guava:guava")
    implementation("io.projectreactor:reactor-core:3.5.3")
    implementation("io.arrow-kt:arrow-core")
    implementation("com.aventrix.jnanoid:jnanoid")
    
    testImplementation("io.projectreactor:reactor-test")
}