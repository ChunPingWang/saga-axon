plugins {
    java
    id("io.spring.dependency-management")
}

dependencies {
    // Axon Framework - core only, no Spring dependency for shared kernel
    implementation("org.axonframework:axon-modelling:4.9.3")

    // Validation
    implementation("jakarta.validation:jakarta.validation-api:3.0.2")

    // Lombok
    compileOnly("org.projectlombok:lombok:1.18.30")
    annotationProcessor("org.projectlombok:lombok:1.18.30")

    // Testing
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.1")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
