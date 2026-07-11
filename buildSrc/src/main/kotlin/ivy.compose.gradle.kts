plugins {
    org.jetbrains.kotlin.plugin.compose
    id("ivy.module")
}

android {
    // Compose
    buildFeatures {
        compose = true
    }

    lint {
        disable += "MissingTranslation"
        disable += "ComposeViewModelInjection"
        abortOnError = false
    }

    testOptions {
        unitTests {
            isReturnDefaultValues = true
        }
    }
}

if (project.hasProperty("composeCompilerReports")) {
    composeCompiler {
        reportsDestination = layout.buildDirectory.dir("compose_compiler")
        metricsDestination = layout.buildDirectory.dir("compose_compiler")
    }
}

dependencies {
    implementation(libs.bundles.compose)
    implementation(libs.cashapp.molecule.runtime)

    lintChecks(libs.slack.lint.compose)
}
