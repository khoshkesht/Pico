plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "ir.hooshamooz.app"
    compileSdk = 36
    buildToolsVersion = "36.0.0"
    defaultConfig {
        applicationId = "picko.mokho.com"
        minSdk = 26
        targetSdk = 36
        versionCode = 10
        versionName = "0.8.0"
    }
    buildFeatures { compose = true }
    androidResources { localeFilters += listOf("fa", "en") }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            isShrinkResources = false
        }
    }
    testOptions.unitTests.all {
        it.systemProperty("stageContentFile", rootProject.file("doc/content/stage-01.json").absolutePath)
        it.systemProperty("stageTwoContentFile", rootProject.file("doc/content/stage-02.json").absolutePath)
        it.systemProperty("stageThreeContentFile", rootProject.file("doc/content/stage-03.json").absolutePath)
        it.systemProperty("stageFourContentFile", rootProject.file("doc/content/stage-04.json").absolutePath)
        it.systemProperty("stageFiveContentFile", rootProject.file("doc/content/stage-05.json").absolutePath)
        it.systemProperty("stageSixContentFile", rootProject.file("doc/content/stage-06.json").absolutePath)
        it.systemProperty("stageSevenContentFile", rootProject.file("doc/content/stage-07.json").absolutePath)
        it.systemProperty("stageEightContentFile", rootProject.file("doc/content/stage-08.json").absolutePath)
        it.systemProperty("dailyContentFile", rootProject.file("doc/content/daily-challenges.json").absolutePath)
    }
    sourceSets["main"].assets.srcDir(layout.buildDirectory.dir("generated/contentAssets"))
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

val prepareContentAssets by tasks.registering(Sync::class) {
    from(rootProject.file("doc/content")) { include("stage-*.json", "daily-challenges.json") }
    into(layout.buildDirectory.dir("generated/contentAssets/content"))
}
tasks.named("preBuild").configure { dependsOn(prepareContentAssets) }

kotlin { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) } }

dependencies {
    implementation(project(":engine"))
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.tracing:tracing:1.2.0")
    implementation("androidx.annotation:annotation-experimental:1.5.0")
    implementation(platform("androidx.compose:compose-bom:2025.11.01"))
    implementation("androidx.activity:activity-compose:1.11.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.4")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.4")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.animation:animation")
    implementation("androidx.compose.material3:material3")
    debugImplementation("androidx.compose.ui:ui-tooling")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")
}
