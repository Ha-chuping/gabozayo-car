pluginManagement { repositories { google(); mavenCentral(); gradlePluginPortal() } }
dependencyResolutionManagement {
    repositories {
        google(); mavenCentral()
        // Pleos SDK 저장소 (ai.pleos.playground 와 그 하위 그룹, 예: ai.pleos.playground.security)
        maven {
            url = uri("https://nexus-playground.pleos.ai/repository/maven-releases/")
            content { includeGroupByRegex("ai\\.pleos\\.playground.*") }
        }
    }
}
rootProject.name = "GabozayoCar"
include(":core", ":car")
