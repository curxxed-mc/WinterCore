plugins {
    `java-library`
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(8))
    }
}

repositories {
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
    maven { url = uri("https://libraries.minecraft.net/") }
    maven { url = uri("https://hub.spigotmc.org/nexus/content/repositories/public/") }
    maven { url = uri("https://repo.dmulloy2.net/repository/public/") }
    maven { url = uri("https://oss.sonatype.org/content/groups/public/") }
    maven { url = uri("https://repo.extendedclip.com/content/repositories/placeholderapi/") }
    maven { url = uri("https://repo.viaversion.com/") }
}

dependencies {
    compileOnly("org.jspecify:jspecify:1.0.0")
    compileOnly("org.projectlombok:lombok:1.18.32")
    annotationProcessor("org.projectlombok:lombok:1.18.32")
    compileOnly("com.viaversion:viaversion:4.8.1")
    compileOnly("org.spigotmc:spigot-api:1.8.8-R0.1-SNAPSHOT")
    compileOnly("me.clip:placeholderapi:2.11.6")
    compileOnly(files("libs/spigot-1.7.10-SNAPSHOT-b1657.jar"))
    compileOnly(files("libs/WinterSpigot-1.8.8-b2.0.7.jar"))
    implementation("org.mongodb:mongodb-driver-sync:3.12.14")
    implementation("com.mojang:brigadier:1.3.10")
    implementation("redis.clients:jedis:5.2.0")
    implementation("org.slf4j:slf4j-simple:1.7.36")
    implementation("com.squareup.okhttp3:okhttp:5.0.0-alpha.14")
    implementation("com.google.guava:guava:21.0")
    implementation("com.warrenstrange:googleauth:1.5.0")
}

tasks {
    shadowJar {
        relocate("com.google.common", "net.curxxed.dev.wintercore.libs.google.common")
        relocate("redis.clients", "net.curxxed.dev.wintercore.libs.redis")
        minimize()

        archiveClassifier.set("")
    }

    withType<JavaCompile> {
        options.encoding = "UTF-8"
    }

    build {
        dependsOn(shadowJar)
    }
}