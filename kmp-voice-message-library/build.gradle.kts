import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKmpLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {

    targets.all {
        compilations.all {
            compileTaskProvider.configure {
                compilerOptions {
                    //Ignore expect actual warnings
                    freeCompilerArgs.add("-Xexpect-actual-classes")
                }
            }
        }
    }

    jvm()

    android {
        namespace = "io.github.lerchenflo.voicemessages"
        compileSdk = 37
        minSdk = 28

        withJava() // enable java compilation support
        withHostTestBuilder {}.configure {}
        withDeviceTestBuilder {
            sourceSetTreeName = "test"
        }

        compilerOptions {
            jvmTarget = JvmTarget.JVM_21
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
            implementation(libs.kotlinx.io.core)

            //For icons
            implementation(libs.material.icons.extended)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}
