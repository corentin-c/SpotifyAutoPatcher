pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

val localProperties = java.util.Properties()
val localPropertiesFile = File(rootDir, "local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(localPropertiesFile.inputStream())
}

val gprUser: String? = localProperties.getProperty("gpr.user")
val gprKey: String? = localProperties.getProperty("gpr.key")


dependencyResolutionManagement {
    repositories {
        maven { url = uri("https://jitpack.io") }
        maven {
            url = uri("https://maven.pkg.github.com/ReVanced/revanced-patcher")
            credentials {
                username = localProperties.getProperty("gpr.user") as String?
                password = localProperties.getProperty("gpr.key") as String?
            }
        }
        google()
        mavenCentral()
    }
}

rootProject.name = "AntiSplit M"
include(":app")
