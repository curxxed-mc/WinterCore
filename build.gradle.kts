plugins {
    `java-library`
    id("com.gradleup.shadow") version "8.3.11"
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
    compileOnly("org.spigotmc:spigot-api:1.8.8-R0.1-SNAPSHOT")
    compileOnly("me.clip:placeholderapi:2.11.6")
    implementation("org.mongodb:mongodb-driver-sync:3.12.14")
    implementation("redis.clients:jedis:5.2.0")
    implementation("com.squareup.okhttp3:okhttp:5.0.0-alpha.14")
    implementation("com.warrenstrange:googleauth:1.5.0")
    testImplementation("junit:junit:4.13.2")
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.compilerArgs.addAll(listOf("-Xlint:deprecation", "-Xlint:unchecked"))
}

tasks.shadowJar {
    archiveClassifier.set("")
    relocate("redis.clients", "net.curxxed.dev.wintercore.libs.redis")
    minimize()
}

tasks.assemble {
    dependsOn(tasks.shadowJar)
}
