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
        // Slack's ParameterOrderDetector throws a NullPointerException under this toolchain
        // (getText(...) must not be null); crashes lintAnalyzeRelease outright rather than
        // reporting findings. See github.com/slackhq/compose-lint-rules.
        disable += "ComposeParameterOrder"
        abortOnError = false
    }

    testOptions {
        unitTests {
            isReturnDefaultValues = true
        }
    }
}

// :temp:old-design crashes the Compose compiler's composables.txt printer with a
// NoSuchElementException in IrSourcePrinter.printReceiver (a compiler bug, not a real
// compile error - `compileReleaseKotlin` succeeds fine without report generation). Since
// it's legacy/temp code slated for removal, skip report generation for it instead of
// working around the upstream compiler bug.
if (project.hasProperty("composeCompilerReports") && project.path != ":temp:old-design") {
    composeCompiler {
        reportsDestination = layout.buildDirectory.dir("compose_compiler")
        metricsDestination = layout.buildDirectory.dir("compose_compiler")
    }
}

dependencies {
    implementation(platform(libs.compose.bom.alpha))
    implementation(libs.bundles.compose)
    implementation(libs.cashapp.molecule.runtime)

    lintChecks(libs.slack.lint.compose)
}
