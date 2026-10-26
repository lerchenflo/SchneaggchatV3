plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.google.services)

}

kotlin {
    jvmToolchain(25)
}

java {
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
}

android {
    namespace = "org.lerchenflo.androidApp"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "org.lerchenflo.schneaggchatv3mp"
        minSdk = 28
        targetSdk = 37
        versionCode = 39
        versionName = "3.0.19"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {

        release {
            optimization {
                enable = true
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }


        debug {
            //Only for debug build
            applicationIdSuffix = ".debug"
        }
    }

    buildFeatures {
        compose = true
    }

    /*
    composeOptions {
        kotlinCompilerExtensionVersion = composeVersion
    }

     */
}

//jvmToolchain(17)

dependencies {
    implementation(projects.composeApp)

    // Compose versions come only from Compose Multiplatform (via composeApp). No Compose BOM here:
    // a newer BOM would raise androidx Compose on Android above what CMP is built for.

    // In-app updates from the Play Store (MainActivity)
    implementation(libs.app.update.ktx)

    // setContent { App() } in MainActivity
    implementation(libs.androidx.activity.compose)


    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // splash screen
    implementation(libs.androidx.core.splashscreen)
}