plugins {
    kotlin("jvm")
    id("com.google.devtools.ksp")
}

// Makes generated code visible to IDE
kotlin.sourceSets.main {
    kotlin.srcDirs(
        file("$buildDir/generated/ksp/main/kotlin"),
    )
}

ksp {
    arg("sourcePackage", "shiny.namespace")
    arg("targetPackage", "old.namespace")
}

dependencies {
    ksp(project(":processor"))
}
