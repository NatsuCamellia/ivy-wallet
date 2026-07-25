plugins {
    id("ivy.kotlin-android")
    id("dagger.hilt.android.plugin")
    id("com.google.devtools.ksp")
}

dependencies {
    implementation(libs.bundles.hilt)
    ksp(catalog.library("hilt-compiler"))

    constraints {
        // androidx.hilt:hilt-work transitively pulls work-runtime:2.3.4, which predates
        // WorkManager's FLAG_IMMUTABLE/FLAG_MUTABLE fix for PendingIntents on API 31+ and
        // crashes ForceStopRunnable at runtime on modules that don't otherwise depend on a
        // newer androidx.work version. Bump it to the version pinned in the catalog.
        implementation("androidx.work:work-runtime") {
            version { require(libs.versions.androidx.work.get()) }
        }
    }
}
