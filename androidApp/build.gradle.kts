plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.google.services)

}

kotlin {
    jvmToolchain(21)
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
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
        versionCode = 23
        versionName = "3.0.12"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {

        release {
            //isMinifyEnabled = true //Disabled for debugging output logs to work correctly
            //isShrinkResources = true
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


    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.app.update.ktx)
    implementation(libs.androidx.compose.ui.tooling)
    implementation(libs.androidx.compose.ui.tooling.preview)

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)


    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // splash screen
    implementation(libs.androidx.core.splashscreen)

    implementation(libs.firebase.messaging)
}