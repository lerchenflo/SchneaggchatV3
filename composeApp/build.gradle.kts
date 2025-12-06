import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeHotReload)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
    alias(libs.plugins.jetbrains.kotlin.serialization)
    id("com.google.gms.google-services")
}



kotlin {
    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }
    
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
            export("io.github.mirzemehdi:kmpnotifier:1.6.0")
        }
    }

    jvm()


    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
    
    sourceSets {
        androidMain.dependencies {
            implementation(compose.preview)
            implementation(libs.androidx.activity.compose)

            implementation(libs.koin.android)
            implementation(libs.koin.androidx.compose)

            //Netzwerk
            implementation(libs.ktor.client.okhttp)

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
            implementation(libs.jetbrains.compose.navigation)
            implementation(libs.kotlinx.serialization.json)

            //Internet
            implementation(libs.bundles.ktor)

            //Firebase
            api(libs.kmpnotifier)

            //Image picker
            implementation(libs.github.imagepickerkmp)
            //Image refactoring


            //Jwt parsing
            implementation(libs.jwt.kt)

            //DateTime
            implementation(libs.kotlinx.datetime)

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




/*
project.tasks.withType(KotlinCompilationTask::class.java).configureEach {
    if(name != "kspCommonMainKotlinMetadata") {
        dependsOn("kspCommonMainKotlinMetadata")
    }
}

 */




android {
    namespace = "org.lerchenflo.schneaggchatv3mp"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "org.lerchenflo.schneaggchatv3mp"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}


compose.desktop {
    application {
        mainClass = "org.lerchenflo.schneaggchatv3mp.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "org.lerchenflo.schneaggchatv3mp"
            packageVersion = "1.0.0"
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
    debugImplementation(compose.uiTooling)
}

ksp {
    arg("KOIN_USE_COMPOSE_VIEWMODEL","true")
    arg("KOIN_CONFIG_CHECK","true")
}



