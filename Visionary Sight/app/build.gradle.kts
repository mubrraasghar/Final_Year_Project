plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.visionarysight"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.visionarysight"
        minSdk = 24
        targetSdk = 34
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {

    implementation("com.google.mlkit:object-detection:17.0.2")
    implementation("com.google.mlkit:object-detection-custom:17.0.2")
    implementation("com.google.code.gson:gson:2.8.6")
    implementation("com.google.guava:guava:27.1-android")
    implementation ("com.google.firebase:firebase-storage:21.0.0")
    androidTestImplementation("androidx.test:core:1.4.0")
    androidTestImplementation("androidx.test:runner:1.4.0")
    androidTestImplementation("androidx.test:rules:1.4.0")
    androidTestImplementation("androidx.test.ext:junit:1.1.3")
    implementation("androidx.lifecycle:lifecycle-livedata:2.3.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel:2.3.1")

    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.annotation:annotation:1.2.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    implementation("com.google.android.odml:image:1.0.0-beta1")
    implementation("androidx.core:core-ktx:1.12.0")

    implementation("androidx.camera:camera-core:1.2.0")
    implementation("androidx.camera:camera-camera2:1.2.0")
    implementation("androidx.camera:camera-lifecycle:1.2.0")
    implementation("androidx.camera:camera-view:1.2.0")
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")

    implementation(platform("com.google.firebase:firebase-bom:33.1.2"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-auth:21.0.1")
    implementation("com.google.android.gms:play-services-auth:19.2.0")

    implementation("com.google.android.material:material:1.5.0")

    implementation("com.github.bumptech.glide:glide:4.14.2")
    implementation(libs.play.services.location)
    implementation(libs.play.services.vision.common)
    implementation(libs.play.services.vision)
    implementation(libs.room.common)
    implementation(libs.room.runtime)

    annotationProcessor("com.github.bumptech.glide:compiler:4.14.2")
    implementation("com.google.android.libraries.places:places:3.3.0")

    implementation("com.google.firebase:firebase-database:20.0.3")
    implementation("com.google.firebase:firebase-firestore:24.0.2")
    implementation ("com.squareup.okhttp3:logging-interceptor:5.0.0-alpha.11")

    implementation("com.squareup.okhttp3:okhttp:4.9.3")

    implementation("com.google.android.gms:play-services-maps:17.0.0")
    implementation("com.google.android.libraries.places:places:2.4.0")

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    annotationProcessor(libs.room.compiler)
}
