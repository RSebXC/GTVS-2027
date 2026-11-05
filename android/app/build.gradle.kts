plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }
android {
 namespace = "gt.edu.usac.gtvs"
 compileSdk = 35
 buildFeatures { buildConfig = true }
 defaultConfig {
  applicationId = "gt.edu.usac.gtvs"
  minSdk = 26
  targetSdk = 35
  versionCode = 3
  versionName = "2.0.0-demo.2"
  buildConfigField("String","API_URL","\"https://br-morning-resonance-belnuka7-gtvsapi.compute.c-9.us-east-2.aws.neon.tech/api\"")
  buildConfigField("String","AUTH_WEB_URL","\"https://rsebxc.github.io/GTVS-2027/?mobile-auth=1\"")
 }
 compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
 kotlinOptions { jvmTarget = "17" }
}
