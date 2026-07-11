package com.ivy.ui

import dalvik.system.VMRuntime
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.spyk
import io.mockk.unmockkStatic
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * Paparazzi 2.0.0-alpha05 bundles layoutlib 16.2.1, whose `VMRuntime_Delegate` is missing stubs
 * for `setThreadNiceness`/`getThreadNiceness`. At compileSdk >= 36, `android.os.Process
 * .setThreadPriority` unconditionally routes through `VMRuntime.setThreadNiceness`, which calls
 * `Thread.setPosixNicenessInternal` - a method that only exists on ART, not on the desktop JVM
 * Paparazzi runs tests on. Any `android.os.HandlerThread` started during a test (Paparazzi/Compose
 * spin up several) crashes with `NoSuchMethodError` before it can start looping.
 *
 * Fixed upstream in layoutlib 16.2.3 (not consumable here: it also removes `Bridge.prepareThread`,
 * which Paparazzi 2.0.0-alpha05 calls directly, so forcing that layoutlib version trades one crash
 * for another). No Paparazzi release has picked up the layoutlib fix yet.
 *
 * This rule stubs `VMRuntime.getRuntime()` for the duration of the test so `setThreadNiceness`/
 * `getThreadNiceness` never reach the missing method, letting `HandlerThread.run()` complete
 * normally (it otherwise dies before calling `Looper.loop()`).
 *
 * https://github.com/cashapp/paparazzi/issues/2342
 * TODO Remove once Paparazzi ships with layoutlib >= 16.2.3.
 *
 * Duplicated from shared/ui/testing's copy: shared/ui/core cannot depend on shared/ui/testing
 * (shared/ui/testing already depends on shared/ui/core), so this small rule is kept in sync by hand.
 */
class PosixNicenessWorkaroundRule : TestRule {
    override fun apply(base: Statement, description: Description): Statement = object : Statement() {
        override fun evaluate() {
            val patchedRuntime = spyk(VMRuntime.getRuntime()) {
                // Pretend the niceness-API call already "succeeded" so Process.setThreadPriority
                // returns immediately instead of falling through to its legacy path, which can
                // throw SecurityException on this JVM for elevated thread priorities.
                every { setThreadNiceness(any(), any()) } returns true
                every { getThreadNiceness(any()) } returns 0
            }
            mockkStatic(VMRuntime::class)
            every { VMRuntime.getRuntime() } returns patchedRuntime
            try {
                base.evaluate()
            } finally {
                unmockkStatic(VMRuntime::class)
            }
        }
    }
}
