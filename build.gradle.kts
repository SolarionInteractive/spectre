plugins {
    id("java")
    id("com.gradleup.shadow") version "9.3.0"
}

group = "dev.solarion"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven {
        name = "hytale-release"
        url = uri("https://maven.hytale.com/release")
    }
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    compileOnly("com.hypixel.hytale:Server:2026.02.06-aa1b071c2")
}

tasks.test {
    useJUnitPlatform()
}

tasks.shadowJar {
    mergeServiceFiles()
    minimize()
}

tasks.build {
    dependsOn(tasks.shadowJar)
}