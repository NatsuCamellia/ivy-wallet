import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters

/**
 * Lets only one KSP task run at a time. Two of them in parallel (different modules, 2 workers) sometimes end
 * without writing the Hilt classes of a module: the build passes, but a ViewModel of that module crashes with
 * "Cannot create an instance of class ..." at runtime.
 */
abstract class KspSerialLock : BuildService<BuildServiceParameters.None>
