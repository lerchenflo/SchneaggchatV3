rootProject.name = "SchneaggchatV3mp"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        // includeGroupByRegex("x(\\..*)?") = the group and its subgroups (stable replacement for the incubating includeGroupAndSubgroups)
        google {
            mavenContent {
                includeGroupByRegex("androidx(\\..*)?")
                includeGroupByRegex("com\\.android(\\..*)?")
                includeGroupByRegex("com\\.google(\\..*)?")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupByRegex("androidx(\\..*)?")
                includeGroupByRegex("com\\.android(\\..*)?")
                includeGroupByRegex("com\\.google(\\..*)?")
            }
        }
        mavenCentral()
        maven { url = uri("https://www.jitpack.io" ) }
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

include(":composeApp")
include(":androidApp")
include(":kmp-voice-message-library")
