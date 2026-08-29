import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

// The Kotlin Android plugin is deliberately absent. AGP 9 has built-in Kotlin
// support and rejects it.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
}

/**
 * The language hygiene gate. Two standing rules apply to everything a person
 * reads in this project: no em dashes, and American English spelling. Spelling
 * cannot be gated mechanically without false positives, but the em dash can be,
 * so it is. The gate scans source, resources, and the living documents. The
 * licenses directory is excluded because license texts are quoted verbatim.
 */
abstract class EmDashGate : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sources: ConfigurableFileCollection

    @TaskAction
    fun check() {
        val offenders = sources.files
            .filter { it.isFile && it.readText().contains('—') }
            .map { it.relativeTo(project.rootDir).path }
        if (offenders.isNotEmpty()) {
            throw GradleException(
                "Em dashes found, and the project bans them everywhere: " +
                    offenders.joinToString(", ")
            )
        }
    }
}

tasks.register<EmDashGate>("emDashGate") {
    sources.from(
        fileTree(rootDir) {
            include("*.md")
            include("app/src/**/*.kt")
            include("app/src/**/*.xml")
        }
    )
}
