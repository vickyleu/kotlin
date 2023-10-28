extensions.add("gradleCompat", objects.newInstance<GradleCompatExtension>(project))
extensions.extraProperties["kotlin.stdlib.default.dependency"] = "false"

dependencies {
    "compileOnly"(kotlinStdlib())
}
configureKotlinCompileTasksGradleCompatibility()
