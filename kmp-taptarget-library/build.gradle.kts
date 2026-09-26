import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKmpLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {

    jvm()

    android {
        namespace = "io.github.lerchenflo.taptarget"
        compileSdk = 37
        minSdk = 28

        compilerOptions {
            //11 so consumers with an older jvmTarget (mocatering) can use the same module
            jvmTarget = JvmTarget.JVM_11
        }
    }

    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            implementation(libs.runtime)
            implementation(libs.foundation)
            implementation(libs.material3)
            implementation(libs.ui)
            implementation(libs.kotlinx.coroutines)

            //StringResource overloads of the tour DSL
            api(libs.components.resources)

            //For icons
            implementation(libs.material.icons.extended)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}
