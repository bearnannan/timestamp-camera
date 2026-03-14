plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services")
    id("com.google.firebase.appdistribution")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.example.timestampcamera"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.timestampcamera"
        minSdk = 24
        targetSdk = 35
        versionCode = 55
        versionName = "2.35"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("debug")

            configure<com.google.firebase.appdistribution.gradle.AppDistributionExtension> {
                appId = "1:1082083969948:android:344bb0f07a976735805659"
                artifactType = "APK"
                groups = "testers"
                
                // --- เพิ่มบรรทัดนี้ครับ ---
                // serviceCredentialsFile = "app/login.json" // REMOVED: File does not exist, using default auth 
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "META-INF/DEPENDENCIES"
        }
    }
}

dependencies {

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-process:2.8.7")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.documentfile:documentfile:1.0.1")
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    
    // Coil
    implementation("io.coil-kt:coil-compose:2.7.0")

    // CameraX
    val cameraxVersion = "1.4.1"
    implementation("androidx.camera:camera-core:$cameraxVersion")
    implementation("androidx.camera:camera-camera2:$cameraxVersion")
    implementation("androidx.camera:camera-lifecycle:$cameraxVersion")
    implementation("androidx.camera:camera-view:$cameraxVersion")
    implementation("androidx.camera:camera-extensions:$cameraxVersion")
    implementation("androidx.camera:camera-video:$cameraxVersion")
    implementation("androidx.camera:camera-effects:$cameraxVersion")

    // Compose Lifecycle & LiveData
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.compose.runtime:runtime-livedata")

    // Location
    implementation("com.google.android.gms:play-services-location:21.3.0")

    // Accompanist (Permissions)
    implementation("com.google.accompanist:accompanist-permissions:0.32.0")
    
    // Icons
    implementation("androidx.compose.material:material-icons-extended")

    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // EXIF
    implementation("androidx.exifinterface:exifinterface:1.3.7")

    // Zxing (QR Code)
    implementation("com.google.zxing:core:3.5.1") 
    
    // ML Kit (Image Labeling)
    implementation("com.google.mlkit:image-labeling:17.0.9")
    // ML Kit (Image Enhancement)
    implementation("com.google.mlkit:segmentation-selfie:16.0.0-beta6")
    implementation("com.google.mlkit:object-detection:17.0.2")
    implementation("com.google.mlkit:vision-common:17.3.0")
    implementation("com.google.mlkit:common:18.7.0")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1")

    // Google Auth & Drive
    implementation("com.google.android.gms:play-services-auth:21.3.0")
    implementation("com.google.api-client:google-api-client-android:2.2.0")
    implementation("com.google.http-client:google-http-client-android:1.44.1")
    implementation("com.google.apis:google-api-services-drive:v3-rev20230822-2.0.0")
    implementation("com.google.guava:guava:31.1-android") {
        exclude(group = "org.apache.httpcomponents")
    }
    
    // WorkManager
    implementation("androidx.work:work-runtime-ktx:2.10.0")
    
    // Media3 (ExoPlayer)
    val media3Version = "1.5.1"
    implementation("androidx.media3:media3-exoplayer:$media3Version")
    implementation("androidx.media3:media3-ui:$media3Version")
    implementation("androidx.media3:media3-common:$media3Version")
    
    // OSMDroid (Mini-Map) REMOVED

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.12.01"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
