plugins {
    id("java")
    id("com.gradleup.shadow") version "9.3.0"
}

group = "dev.solarion"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    implementation(files("${System.getProperty("user.home")}/AppData/Roaming/Hytale/install/release/package/game/latest/Server/HytaleServer.jar"))
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