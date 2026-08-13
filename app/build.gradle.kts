plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.sazanx.mouseconfigurator[span_0](start_span)"[span_0](end_span)
    compileSdk = 35[span_1](start_span)[span_1](end_span)

    defaultConfig {
        applicationId = "com.sazanx.mouseconfigurator[span_2](start_span)"[span_2](end_span)
        minSdk = 26[span_3](start_span)[span_3](end_span)
        targetSdk = 35[span_4](start_span)[span_4](end_span)
        versionCode = 5[span_5](start_span)[span_5](end_span)
        versionName = "5.0[span_6](start_span)"[span_6](end_span)
    }

    buildTypes {
        release {
            isMinifyEnabled = false[span_7](start_span)[span_7](end_span)
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),[span_8](start_span)[span_8](end_span)
                "proguard-rules.pro[span_9](start_span)"[span_9](end_span)
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17[span_10](start_span)[span_10](end_span)
        targetCompatibility = JavaVersion.VERSION_17[span_11](start_span)[span_11](end_span)
    }

    kotlinOptions {
        jvmTarget = "17[span_12](start_span)"[span_12](end_span)
    }

    buildFeatures {
        viewBinding = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")[span_13](start_span)[span_13](end_span)
    implementation("androidx.appcompat:appcompat:1.7.0")[span_14](start_span)[span_14](end_span)
    implementation("androidx.activity:activity:1.10.1")[span_15](start_span)[span_15](end_span)
    implementation("com.google.android.material:material:1.12.0")[span_16](start_span)[span_16](end_span)

    implementation("dev.rikka.shizuku:api:13.1.5")[span_17](start_span)[span_17](end_span)
    implementation("dev.rikka.shizuku:provider:13.1.5")[span_18](start_span)[span_18](end_span)
}
