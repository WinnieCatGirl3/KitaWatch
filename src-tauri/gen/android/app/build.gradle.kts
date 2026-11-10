import java.util.Properties
import org.gradle.api.tasks.Copy
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("rust")
}

// Embedded Python runtime for the bundled local services (Kuhi API). The
// plugin comes from the root buildscript classpath (see ../build.gradle.kts),
// so it is applied with apply() rather than the plugins {} block.
apply(plugin = "com.chaquo.python")

val tauriProperties = Properties().apply {
    val propFile = file("tauri.properties")
    if (propFile.exists()) {
        propFile.inputStream().use { load(it) }
    }
}

android {
    compileSdk = 37
    namespace = "com.kitawatch.app"
    defaultConfig {
        manifestPlaceholders["usesCleartextTraffic"] = "false"
        applicationId = "com.kitawatch.app"
        minSdk = 24
        targetSdk = 37
        versionCode = tauriProperties.getProperty("tauri.android.versionCode", "1").toInt()
        versionName = tauriProperties.getProperty("tauri.android.versionName", "1.0")
        // curl_cffi (TLS impersonation, required by the bundled APIs) only ships
        // android_24_arm64_v8a wheels, so arm64 is the only usable ABI here.
        ndk {
            abiFilters += listOf("arm64-v8a")
        }
    }
    buildTypes {
        getByName("debug") {
            manifestPlaceholders["usesCleartextTraffic"] = "true"
            isDebuggable = true
            isJniDebuggable = true
            isMinifyEnabled = false
            packaging {
                jniLibs.keepDebugSymbols.add("*/arm64-v8a/*.so")
                jniLibs.keepDebugSymbols.add("*/armeabi-v7a/*.so")
                jniLibs.keepDebugSymbols.add("*/x86/*.so")
                jniLibs.keepDebugSymbols.add("*/x86_64/*.so")
            }
        }
        getByName("release") {
            optimization {
               enable = true
            }
            proguardFiles(
                *fileTree(".") {
                  include("**/*.pro")
                  exclude("build/**")
                }.files.toTypedArray()
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    buildFeatures {
        buildConfig = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_1_8
    }
}

rust {
    rootDirRel = "../../../"
}

dependencies {
    implementation("androidx.webkit:webkit:1.14.0")
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("androidx.activity:activity-ktx:1.10.1")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.lifecycle:lifecycle-process:2.10.0")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.4")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.0")
}

apply(from = file("tauri.build.gradle.kts"))

// ---------------------------------------------------------------------------
// Embedded Python (Chaquopy)
//
// Android has no sidecar mechanism like desktop (see src-tauri/src/api_sidecar.rs),
// so the Kuhi API runs inside the app process via Chaquopy instead.
//
// Version notes (verified against PyPI and the Chaquopy wheel repo):
// - Python 3.14: newest with curl_cffi cp314 Android wheels; Chaquopy 17.2
//   supports 3.10-3.15. 64-bit only >= 3.12, which is fine (arm64 only below).
// - pydantic<2: pydantic-core (Rust) has NO Android wheels on PyPI or in the
//   Chaquopy repo, so pydantic v1 (pure Python) is required. fastapi 0.115.x is
//   the newest line that still imports with pydantic v1 (0.121+ needs
//   TypeAdapter from v2). Both wheels are py3-none-any.
// - curl-cffi: ships android_24_arm64_v8a wheels; needs cffi>=2.1.1, which is
//   not in the Chaquopy repo (max 1.17.1) nor on PyPI-for-Android, but is
//   hosted by the py-extra-wheels index below.
// Keep these pins Android-only: api/anime-api/requirements.txt and
// proxy/requirements.txt are installed by release.yml for the DESKTOP builds.
// ---------------------------------------------------------------------------
configure<com.chaquo.python.ChaquopyExtension> {
    defaultConfig {
        version = "3.14"
        pip {
            options("--extra-index-url", "https://ririv.github.io/py-extra-wheels/")
            // install() takes exactly ONE requirement per call (verified in the
            // plugin bytecode: multi-arg calls throw "Invalid pip install format").
            install("fastapi==0.115.12")
            install("pydantic<2")
            install("uvicorn")
            install("httpx")
            install("python-multipart")
            install("curl-cffi>=0.16.2")
        }
    }
}

// The Kuhi API is cloned into api/anime-api/ by `npm run setup:api` (gitignored
// there). Copy the runtime files into the Python source set so the APK always
// carries a consistent snapshot; kitawatch_server.py stays versioned.
val kuhiApiDir = rootDir.resolve("../../../api/anime-api")
val syncKuhiPython = tasks.register<Copy>("syncKuhiPython") {
    from(kuhiApiDir) {
        include("api.py", "src/**", "assets/**")
    }
    into(layout.projectDirectory.dir("src/main/python"))
    // Deliberately no onlyIf/exists() guard: a config-time file check would not
    // invalidate the configuration cache once api/anime-api is cloned later, and
    // the lambda would not be config-cache serializable anyway. Copying from a
    // missing directory just yields no files — run `npm run setup:api` first.
}
tasks.named("preBuild") {
    dependsOn(syncKuhiPython)
}
// Chaquopy's per-variant source merge reads src/main/python directly, so it needs
// the explicit edge (preBuild alone does not cover it). Configuration-time hook,
// nothing is captured on the task itself.
tasks.configureEach {
    if (name.startsWith("merge") && name.endsWith("PythonSources")) {
        dependsOn(syncKuhiPython)
    }
}
