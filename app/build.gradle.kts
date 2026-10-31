plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

android {
    namespace = "org.myhousehold.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "org.myhousehold.app"
        // 26 : la quasi-totalité des téléphones en service, et les API
        // de date qu'on utilise sans acrobaties.
        minSdk = 26
        targetSdk = 35
        versionCode = 4
        versionName = "0.4.0"
    }

    // Une seule clé pour toute la vie de l'app : Android refuse une mise
    // à jour signée par une autre. Elle n'est jamais dans le dépôt —
    // l'intégration continue la reçoit par ses secrets, au moment de
    // publier (voir docs/updates.md).
    signingConfigs {
        create("publication") {
            val coffre = System.getenv("MH_KEYSTORE_FILE")
            if (coffre != null) {
                storeFile = file(coffre)
                storePassword = System.getenv("MH_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("MH_KEY_ALIAS")
                keyPassword = System.getenv("MH_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            // Pas de réduction de code tant qu'aucune version de
            // publication n'a été essayée sur un téléphone : R8 casse
            // volontiers ce qui passe par réflexion, et cela ne se voit
            // qu'à l'exécution.
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (System.getenv("MH_KEYSTORE_FILE") != null) {
                signingConfig = signingConfigs.getByName("publication")
            }
        }
        debug {
            applicationIdSuffix = ".debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    buildFeatures {
        compose = true
        // Pour lire la version affichée dans « À propos » sans la
        // recopier à la main dans le code.
        buildConfig = true
    }

    // Room écrit le schéma de la base à chaque compilation : une
    // migration oubliée se voit alors en revue, dans le diff, et non
    // chez l'utilisateur au premier lancement.
    ksp {
        arg("room.schemaLocation", "$projectDir/schemas")
    }

    sourceSets {
        getByName("androidTest") {
            assets.srcDir("$projectDir/schemas")
        }
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

dependencies {
    implementation(project(":core"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.datetime)
    implementation(libs.kotlinx.serialization.json)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons)
    implementation(libs.androidx.navigation.compose)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.datastore.preferences)

    implementation(libs.okhttp)
    implementation(libs.zxing.android.embedded)
    implementation(libs.rich.editor)

    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(kotlin("test"))
}
