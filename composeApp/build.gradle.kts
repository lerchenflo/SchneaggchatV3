
import io.github.frankois944.spmForKmp.swiftPackageConfig
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
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
    alias(libs.plugins.koin.compiler)

    id("io.github.frankois944.spmForKmp") //Swift package manager for map
}


//Maplibre ships one desktop render runtime per operating system and CPU architecture, and only the
//one matching the machine that runs the app can be loaded.
val desktopHostIsMac = System.getProperty("os.name").lowercase().startsWith("mac")
val desktopHostIsWindows = System.getProperty("os.name").lowercase().startsWith("windows")
val desktopHostIsArm64 = System.getProperty("os.arch").lowercase() in setOf("aarch64", "arm64")


kotlin {
    jvmToolchain(25)

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
}

java {
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
}

kotlin {
//    androidTarget {
//        @OptIn(ExperimentalKotlinGradlePluginApi::class)
//        compilerOptions {
//            jvmTarget.set(JvmTarget.JVM_17)
//        }
//    }


    android {
        compileSdk = 37
        minSdk = 28
        namespace = "org.lerchenflo.schneaggchatv3mp.androidApp"
        androidResources { enable = true }

        //experimentalProperties["android.experimental.kmp.enableAndroidResources"] = true

//        @OptIn(ExperimentalKotlinGradlePluginApi::class)
//        compilerOptions {
//            freeCompilerArgs.addAll("-Xexpect-actual-classes")
//        }
    }

    
    listOf(
        //iosX64(), //Not supported anymore
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->

        /*
        //Maplibre map export for ios
        iosTarget.swiftPackageConfig(cinteropName = "spmMaplibre") {
            dependency {
                remotePackageVersion(
                    url = URI("https://github.com/maplibre/maplibre-gl-native-distribution.git"),
                    products = { add("MapLibre") },

                    version = "6.17.1",
                )

            }
        }

         */




        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
            binaryOption("bundleId", "org.lerchenflo.schneaggchatv3mp.SchneaggchatV3mp") //Removes compile warning
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

            //Firebase Cloud Messaging
            implementation(libs.firebase.messaging)

            // Location (FusedLocationProviderClient)
            implementation(libs.play.services.location)

            //map render backend
            runtimeOnly(libs.maplibre.compose.runtime.vulkan.android)

            //EXIF orientation of picked images
            implementation(libs.androidx.exifinterface)

        }


        commonMain.dependencies {
            implementation(libs.runtime)
            implementation(libs.foundation)
            implementation(libs.material3)
            implementation(libs.material.icons.extended)
            implementation(libs.ui)
            implementation(libs.components.resources)
            implementation(libs.ui.tooling.preview)
            //implementation(libs.androidx.lifecycle.viewmodelCompose)
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

            // Permissions
            //api(libs.moko.permissions)
            //api(libs.moko.permissions.compose)
            //api(libs.moko.permissions.microphone)

            //Old navigation (Not used anymore)
            //implementation(libs.jetbrains.compose.navigation)
            //implementation(libs.kotlinx.serialization.json)

            //Internet
            implementation(libs.bundles.ktor)


            //Image picker
            implementation(libs.github.imagepickerkmp)

            // Voice messages (local module, not published)
            implementation(projects.kmpVoiceMessageLibrary)

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

            //Swipeable
            implementation(libs.swipeable.kmp)

            //Reordable Column
            implementation(libs.reorderable)

            //Roadmap timeline
            implementation(libs.jetlime)

            //Sharedprefs
            api(libs.datastore.preferences)
            api(libs.datastore)

            //Secure sharedprefs
            implementation(libs.ksafe)
            implementation(libs.ksafe.compose)

            // Markdown
            //implementation(libs.mikepenzMultiplatformMarkdown)
            implementation(libs.mikepenzMultiplatformMarkdownM3)

            //Base64 encoding
            implementation(libs.base64)

            //Image loading async
            implementation(libs.coil3.coil.compose)

            //map
            implementation(libs.maplibre.compose)
            implementation(libs.maplibre.compose.material3)

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

            //map render backend for the host this build runs on
            runtimeOnly(
                when {
                    desktopHostIsMac -> libs.maplibre.compose.runtime.metal.macos.arm64
                    desktopHostIsWindows && desktopHostIsArm64 -> libs.maplibre.compose.runtime.vulkan.windows.arm64
                    desktopHostIsWindows -> libs.maplibre.compose.runtime.vulkan.windows.x64
                    desktopHostIsArm64 -> libs.maplibre.compose.runtime.vulkan.linux.arm64
                    else -> libs.maplibre.compose.runtime.vulkan.linux.x64
                }
            )

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



//Add version info to desktop builds
val desktopVersion = "3.0.17"

compose.desktop {
    application {
        mainClass = "org.lerchenflo.schneaggchatv3mp.MainKt"

        //The maplibre desktop bindings reach native code through the FFM API
        jvmArgs += "--enable-native-access=ALL-UNNAMED"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb, TargetFormat.Rpm)
            packageName = "Schneaggchat v3"
            packageVersion = desktopVersion
            copyright = "© 2026"
            vendor = "lerchenflo"
            modules("jdk.unsupported")

            linux {
                // Linux override. Keep this strictly lowercase with no spaces.
                packageName = "schneaggchat-v3"

                shortcut = true
                menuGroup = "Chat"
                iconFile.set(project.file("src/commonMain/composeResources/files/schneaggchat_logo_v3_transparent.png"))
            }

            windows {
                iconFile.set(project.file("\\src\\commonMain\\composeResources\\files\\schneaggchat_logo_v3_transparent.ico"))

                perUserInstall = true

                upgradeUuid = "7edd54f0-9959-4da2-9e7a-7512f0e042ec"

                menu = true
                shortcut = true

                // ./gradlew packageDistributionForCurrentOS

            }

            macOS {
                // Pro-tip: If you target macOS, set a bundleID without spaces here.
                // Otherwise, macOS might try to generate one from the spaced packageName and fail.
                bundleID = "org.lerchenflo.schneaggchatv3"
            }
        }
        buildTypes.release.proguard {
            isEnabled.set(false) // disable ProGuard
        }
    }
}

tasks.withType<Jar> {
    manifest {
        attributes(
            "Implementation-Title" to "Schneaggchat",
            "Implementation-Version" to desktopVersion,
            "Implementation-Vendor" to "lerchenflo"
        )
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

dependencies {
    // Apply KSP processors to all configured targets
    listOf("Android", "IosArm64", "IosSimulatorArm64", "Jvm").forEach { target ->
        add("ksp$target", libs.room.compiler)
        //add("ksp$target", libs.koin)
    }
}
