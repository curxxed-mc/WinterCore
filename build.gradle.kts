plugins {
    `java-library`
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(8))
    }
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

dependencies {
    compileOnly("org.jspecify:jspecify:1.0.0")
    compileOnly("org.projectlombok:lombok:1.18.32")
    annotationProcessor("org.projectlombok:lombok:1.18.32")
    compileOnly("com.viaversion:viaversion:4.8.1")
    compileOnly("org.spigotmc:spigot-api:1.8.8-R0.1-SNAPSHOT")
    compileOnly("me.clip:placeholderapi:2.11.6")
    //compileOnly(files("libs/spigot-1.7.10-SNAPSHOT-b1657.jar"))
    //compileOnly(files("libs/WinterSpigot-1.8.8-b2.0.7.jar"))
    implementation("org.mongodb:mongodb-driver-sync:3.12.14")
    implementation("redis.clients:jedis:5.2.0")
    implementation("org.slf4j:slf4j-simple:1.7.36")
    implementation("com.squareup.okhttp3:okhttp:5.0.0-alpha.14")
    implementation("com.google.guava:guava:21.0")
    implementation("com.warrenstrange:googleauth:1.5.0")
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}

tasks.shadowJar {
    archiveClassifier.set("")
    relocate("com.google.common", "net.curxxed.dev.wintercore.libs.google.common")
    relocate("redis.clients", "net.curxxed.dev.wintercore.libs.redis")
    minimize()
}

tasks.assemble {
    dependsOn(tasks.shadowJar)
}
