plugins {
    id("java-library")
    id("idea")
}

//set build group and build version string
group = "net.edeenmc"
version = BuildTools.createVersionString(project)

java.toolchain.languageVersion = JavaLanguageVersion.of(21)

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.release.set(21)
}

tasks.withType<GenerateModuleMetadata>().configureEach {
    enabled = false
}

//replace version placeholder inside fabric.mod.json and neoforge.mods.toml with our built version string
tasks.processResources {
    inputs.property("version", version)

    filesMatching(listOf("fabric.mod.json", "META-INF/neoforge.mods.toml")) {
        expand(mapOf("version" to inputs.properties["version"]))
    }
}