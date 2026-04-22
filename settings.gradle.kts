pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        mavenCentral()
        maven("https://jitpack.io")
        maven("https://libraries.minecraft.net/")
        maven("https://hub.spigotmc.org/nexus/content/repositories/public/")
        maven("https://repo.dmulloy2.net/repository/public/")
        maven("https://oss.sonatype.org/content/groups/public/")
        maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
        maven("https://repo.viaversion.com/")
    }
}

rootProject.name = "WinterCore"
