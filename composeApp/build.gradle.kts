@file:OptIn(ExperimentalSpmForKmpFeature::class)

import io.github.frankois944.spmForKmp.swiftPackageConfig
import io.github.frankois944.spmForKmp.utils.ExperimentalSpmForKmpFeature
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import java.net.URI

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeHotReload)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
    alias(libs.plugins.jetbrains.kotlin.serialization)
    alias(libs.plugins.androidKmpLibrary)
    id("io.github.frankois944.spmForKmp")
}


fun detectTarget(): String {
    val hostOs = when (val os = System.getProperty("os.name").lowercase()) {
        "mac os x" -> "macos"
        else -> os.split(" ").first()
    }
    val hostArch = when (val arch = System.getProperty("os.arch").lowercase()) {
        "x86_64" -> "amd64"
        "arm64" -> "aarch64"
        else -> arch
    }
    val renderer = when (hostOs) {
        "macos" -> "metal"
        else -> "opengl"
    }
    return "${hostOs}-${hostArch}-${renderer}"
}


kotlin {
    jvmToolchain(17)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
//    androidTarget {
//        @OptIn(ExperimentalKotlinGradlePluginApi::class)
//        compilerOptions {
//            jvmTarget.set(JvmTarget.JVM_17)
//        }
//    }


    androidLibrary {
        compileSdk = 36
        minSdk = 26
        namespace = "org.lerchenflo.schneaggchatv3mp.androidApp"
        androidResources { enable = true }

        //experimentalProperties["android.experimental.kmp.enableAndroidResources"] = true

//        @OptIn(ExperimentalKotlinGradlePluginApi::class)
//        compilerOptions {
//            freeCompilerArgs.addAll("-Xexpect-actual-classes")
//        }
    }

    
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->

        iosTarget.swiftPackageConfig(cinteropName = "spmMaplibre") {
            dependency {
                remotePackageVersion(
                    url = URI("https://github.com/maplibre/maplibre-gl-native-distribution.git"),
                    products = { add("MapLibre") },
                    version = "6.17.1",
                )

            }

        }

        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
            export("io.github.mirzemehdi:kmpnotifier:1.6.1")
        }


    }

    jvm()

    //jvmToolchain(17)


    sourceSets {
        androidMain.dependencies {
            implementation(libs.ui.tooling)
            implementation(libs.ui.tooling.preview)
            implementation(libs.androidx.activity.compose)

            implementation(libs.koin.android)
            implementation(libs.koin.androidx.compose)

            //Netzwerk
            implementation(libs.ktor.client.okhttp)

            //App update
            implementation(libs.app.update.ktx)
            //implementation(libs.core.ktx)

        }


        commonMain.dependencies {
            implementation(libs.runtime)
            implementation(libs.foundation)
            implementation(libs.material3)
            implementation(libs.material.icons.extended)
            implementation(libs.ui)
            implementation(libs.components.resources)
            implementation(libs.ui.tooling.preview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)

            //Datenbank
            implementation(libs.room.runtime)
            implementation(libs.sqlite.bundled)


            //Viewmodel / DI
            implementation(libs.sqlite.bundled)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
            api(libs.koin.core)
            implementation(libs.koin.core)
            implementation(libs.koin.compose.viewmodel.navigation)
            api(libs.koin.annotations)

            //Navigation
            implementation(libs.jetbrains.navigation3.ui)
            implementation(libs.jetbrains.lifecycle.viewmodel.nav3)

            //Old navigation (Not used anymore)
            //implementation(libs.jetbrains.compose.navigation)
            //implementation(libs.kotlinx.serialization.json)

            //Internet
            implementation(libs.bundles.ktor)

            //Firebase
            api(libs.kmpnotifier)

            //Image picker
            implementation(libs.github.imagepickerkmp)

            //Jwt parsing
            implementation(libs.jwt.kt)

            //Encryption
            implementation(libs.cryptography.core)
            implementation(libs.cryptography.provider.optimal)

            //DateTime
            implementation(libs.kotlinx.datetime)
            implementation(libs.datetime.wheel.picker)

            //Resizeable screens
            implementation(libs.material3.adaptive)

            //Sharedprefs
            api(libs.datastore.preferences)
            api(libs.datastore)

            // Markdown
            //implementation(libs.mikepenzMultiplatformMarkdown)
            implementation(libs.mikepenzMultiplatformMarkdownM3)

            //Base64 encoding
            implementation(libs.base64)

            //Image loading async
            implementation(libs.coil3.coil.compose)

            //maps
            implementation(libs.maplibre.compose)

        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        nativeMain.dependencies {
            //IOS züg

            //Netzwerk
            implementation(libs.ktor.client.darwin)
        }

        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutinesSwing)
            implementation(libs.ktor.client.okhttp)

        }
    }

    /*
    sourceSets.named("commonMain").configure {
        kotlin.srcDir("build/generated/ksp/metadata/commonMain/kotlin")
    }

     */


}





//project.tasks.withType(KotlinCompilationTask::class.java).configureEach {
//    if(name != "kspCommonMainKotlinMetadata") {
//        dependsOn("kspCommonMainKotlinMetadata")
//    }
//}



compose.desktop {
    application {
        mainClass = "org.lerchenflo.schneaggchatv3mp.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb, TargetFormat.Rpm)
            packageName = "Schneaggchat v3"
            packageVersion = "3.0.0"
            copyright = "© 2026"
            vendor = "lerchenflo"

            linux {
                shortcut = true
                menuGroup = "Chat"
            }

            windows {
                iconFile.set(project.file("\\src\\commonMain\\composeResources\\files\\schneaggchat_logo_v3.ico"))

                perUserInstall = true

                upgradeUuid = "7edd54f0-9959-4da2-9e7a-7512f0e042ec"

                menu = true
                shortcut = true

                // ./gradlew packageDistributionForCurrentOS

            }
        }
    }
}

tasks.register<DefaultTask>("runDesktop") {
    group = "application"
    description = "Runs the Compose Desktop app"

    dependsOn("run") // reuse the Compose Desktop run task
}

room{
    schemaDirectory("$projectDir/schemas")
}

dependencies{
    //add("kspCommonMainMetadata", libs.koin.ksp.compiler)
    ksp(libs.room.compiler)
    //debugImplementation(libs.ui.tooling)
}

ksp {
    arg("KOIN_USE_COMPOSE_VIEWMODEL","true")
    arg("KOIN_CONFIG_CHECK","true")
}