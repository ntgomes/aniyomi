

plugins {
    id("com.android.library")
    kotlin("android")
    kotlin("plugin.serialization")
    id("app.cash.sqldelight")
}

android {
    namespace = "tachiyomi.data"

    defaultConfig {
        consumerProguardFiles("consumer-rules.pro")
    }

    sqldelight {
        databases {
            create("Database") {
                packageName.set("tachiyomi.data")
                dialect(libs.sqldelight.dialects.sql)
                schemaOutputDirectory.set(project.file("./src/main/sqldelight"))
                srcDirs.from(project.file("./src/main/sqldelight"))
            }
            create("AnimeDatabase") {
                packageName.set("tachiyomi.mi.data")
                dialect(libs.sqldelight.dialects.sql)
                schemaOutputDirectory.set(project.file("./src/main/sqldelightanime"))
                srcDirs.from(project.file("./src/main/sqldelightanime"))
            }
        }
    }
}

dependencies {
    implementation(project(":source-api"))
    implementation(project(":domain"))
    implementation(project(":core"))

    api(libs.bundles.sqldelight)
}

tasks {
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.freeCompilerArgs += listOf(
            "-Xcontext-receivers",
            "-opt-in=kotlinx.serialization.ExperimentalSerializationApi",
        )
    }
}
