plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("com.google.devtools.ksp") version "1.8.22-1.0.11"
}

android {
    namespace = "com.example.criminalintent.crimeinalntent2"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.criminalintent.crimeinalntent2"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    kotlin {
        jvmToolchain(11)
    }
}

dependencies {
    implementation("androidx.recyclerview:recyclerview:1.0.0")
    implementation("androidx.lifecycle:lifecycle-extensions:2.0.0")
    implementation("androidx.room:room-runtime:2.5.0")
    ksp("androidx.room:room-compiler:2.5.0")

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}