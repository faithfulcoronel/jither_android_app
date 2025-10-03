// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
}

subprojects {
    configurations.configureEach {
        resolutionStrategy.eachDependency {
            if (requested.group == "com.squareup" && requested.name == "javapoet") {
                useVersion(libs.versions.javapoet.get())
                because("Older JavaPoet versions miss ClassName.canonicalName used by recent tooling")
            }
        }
    }
}
