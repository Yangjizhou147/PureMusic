plugins {
    alias(libs.plugins.androidApplication)
}

android {
    namespace = "com.wintercruel.puremusic"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.wintercruel.puremusic"
        minSdk = 30
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

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation("androidx.media3:media3-exoplayer:1.3.1")
    implementation("androidx.media3:media3-ui:1.3.1")
    implementation("androidx.media3:media3-session:1.3.1")
    implementation("com.github.bumptech.glide:glide:4.12.0")
    implementation("jp.wasabeef:glide-transformations:4.3.0")
    implementation("org.greenrobot:eventbus:3.3.1")

    implementation("com.github.zhengken:LyricViewDemo:v1.2")
    implementation(files("libs\\jaudiotagger-3.0.2-SNAPSHOT.jar"))
    implementation(files("libs\\jaudiotagger-3.0.2-SNAPSHOT-javadoc.jar"))
    implementation("com.squareup.okhttp3:okhttp:4.9.1")
    implementation("androidx.room:room-runtime:2.5.0")
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")
    implementation("com.google.zxing:core:3.3.0")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("com.github.GitHubZJY:AudioVisualizeView:v1.0.0")
    implementation("androidx.media:media:1.6.0")
    annotationProcessor("androidx.room:room-compiler:2.5.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.12.0")
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
