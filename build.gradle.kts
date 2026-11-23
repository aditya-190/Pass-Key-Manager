plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose.compiler) apply false
    alias(libs.plugins.hilt.android) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.room) apply false
    alias(libs.plugins.google.services) apply false
    alias(libs.plugins.firebase.crashlytics.plugin) apply false
    alias(libs.plugins.firebase.perf.plugin) apply false
    alias(libs.plugins.ktlint) apply false
}

// detekt runs as the standalone CLI: the 1.23.x Gradle plugin is built against Kotlin 1.9 and does not
// support this project's Kotlin 2.x compiler, while the CLI has its own isolated classpath.
val detektCli: Configuration by configurations.creating

dependencies {
    detektCli(libs.detekt.cli)
}

val detektConfig = file("config/detekt/detekt.yml")
val detektBaselineFile = file("config/detekt/baseline.xml")
val detektSources = listOf("app/src/main/java", "app/src/test/java", "app/src/androidTest/java")
    .joinToString(",") { file(it).absolutePath }

tasks.register<JavaExec>("detekt") {
    group = "verification"
    description = "Runs detekt with the studio config; pre-existing findings live in config/detekt/baseline.xml."
    mainClass.set("io.gitlab.arturbosch.detekt.cli.Main")
    classpath = detektCli
    inputs.files(detektConfig)
    inputs.dir("app/src")
    val args = mutableListOf(
        "--input", detektSources,
        "--config", detektConfig.absolutePath,
        "--build-upon-default-config",
        "--report", "html:${layout.buildDirectory.get().asFile}/reports/detekt/detekt.html"
    )
    // `--baseline` fails when the file does not exist yet, so only pass it once generated.
    if (detektBaselineFile.exists()) args += listOf("--baseline", detektBaselineFile.absolutePath)
    setArgs(args)
}

tasks.register<JavaExec>("detektBaseline") {
    group = "verification"
    description = "Records current detekt findings in config/detekt/baseline.xml. New code must stay clean."
    mainClass.set("io.gitlab.arturbosch.detekt.cli.Main")
    classpath = detektCli
    setArgs(
        listOf(
            "--input", detektSources,
            "--config", detektConfig.absolutePath,
            "--build-upon-default-config",
            "--baseline", detektBaselineFile.absolutePath,
            "--create-baseline"
        )
    )
}
