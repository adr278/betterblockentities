plugins {
    id("java-library")

    id("fabric-loom") version BuildConfig.LOOM_VERSION
}

base {
    archivesName = "bbe-common"
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(21)
}

tasks.withType<JavaCompile> {
    options.release.set(21)
}

sourceSets {
    val main = getByName("main")
    val api = create("api")

    api.apply {
        java {
            compileClasspath += main.compileClasspath
        }
    }

    main.apply {
        java {
            compileClasspath += api.output
        }
    }
}

repositories {
    maven("https://maven.caffeinemc.net/releases")
    maven("https://maven.caffeinemc.net/snapshots")
    mavenLocal()
}

dependencies {
    minecraft("com.mojang:minecraft:${BuildConfig.MINECRAFT_VERSION}")
    mappings(loom.layered {
        officialMojangMappings()
    })

    modCompileOnly("net.fabricmc:sponge-mixin:0.13.2+mixin.0.8.5")
    modCompileOnly("net.fabricmc:fabric-loader:${BuildConfig.FABRIC_LOADER_VERSION}")

    modImplementation("net.caffeinemc:sodium-fabric:${BuildConfig.SODIUM_VERSION}")

    fun addDependentFabricModule(name: String) {
        modCompileOnly(fabricApi.module(name, BuildConfig.FABRIC_API_VERSION))
    }

    addDependentFabricModule("fabric-api-base")
    addDependentFabricModule("fabric-block-view-api-v2")
    addDependentFabricModule("fabric-renderer-api-v1")
    addDependentFabricModule("fabric-rendering-data-attachment-v1")

    annotationProcessor("io.github.llamalad7:mixinextras-common:0.5.0")
    modCompileOnly("io.github.llamalad7:mixinextras-common:0.5.0")

    modCompileOnly("org.jspecify:jspecify:1.0.0")
    compileOnly("org.checkerframework:checker-qual:3.48.4")
}

loom {
    accessWidenerPath = file("src/main/resources/betterblockentities-common.accesswidener")
}

fun exportSourceSetJava(name: String, sourceSet: SourceSet) {
    val configuration = configurations.create("${name}Java") {
        isCanBeResolved = true
        isCanBeConsumed = true
    }

    val compileTask = tasks.getByName<JavaCompile>(sourceSet.compileJavaTaskName)
    artifacts.add(configuration.name, compileTask.destinationDirectory) {
        builtBy(compileTask)
    }
}

fun exportSourceSetSources(name: String, sourceSet: SourceSet) {
    val configuration = configurations.create("${name}Sources") {
        isCanBeResolved = true
        isCanBeConsumed = true
    }

    val compileTask = tasks.register<Copy>(sourceSet.getTaskName("process", "sources")) {
        from(sourceSet.allSource)
        into(file(project.layout.buildDirectory).resolve("sources").resolve(sourceSet.name))
    }.get()
    artifacts.add(configuration.name, compileTask.destinationDir) {
        builtBy(compileTask)
    }
}

fun exportSourceSetResources(name: String, sourceSet: SourceSet) {
    val configuration = configurations.create("${name}Resources") {
        isCanBeResolved = true
        isCanBeConsumed = true
    }

    val compileTask = tasks.getByName<ProcessResources>(sourceSet.processResourcesTaskName)
    compileTask.apply {
        exclude("**/README.txt")
        exclude("/*.accesswidener")
    }

    artifacts.add(configuration.name, compileTask.destinationDir) {
        builtBy(compileTask)
    }
}

fun exportSourceSet(name: String, sourceSet: SourceSet) {
    exportSourceSetJava(name, sourceSet)
    exportSourceSetSources(name, sourceSet)
    exportSourceSetResources(name, sourceSet)
}

exportSourceSet("commonMain", sourceSets["main"])
exportSourceSet("commonApi", sourceSets["api"])

tasks.jar { enabled = false }

tasks.named("remapJar") {
    enabled = false
}
