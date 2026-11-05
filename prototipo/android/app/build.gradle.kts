plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }
android {
    namespace = "gt.edu.usac.rutascontexto"
    compileSdk = 35
    defaultConfig {
        applicationId = "gt.edu.usac.rutascontexto"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0-demo"
    }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
}
