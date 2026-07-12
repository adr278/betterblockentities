plugins {
    id("java-library")

    id("net.fabricmc.fabric-loom") version BuildConfig.LOOM_VERSION
}

base {
    archivesName = "bbe-common"
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(25)
}

tasks.withType<JavaCompile> {
    options.release.set(25)
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

fun DependencyHandlerScope.fabricModule(name: String) {
    compileOnly(fabricApi.module(name, BuildConfig.FABRIC_API_VERSION))
}

dependencies {
    minecraft("com.mojang:minecraft:${BuildConfig.MINECRAFT_VERSION}")

    compileOnly("net.fabricmc:sponge-mixin:0.13.2+mixin.0.8.5")
    compileOnly("net.fabricmc:fabric-loader:${BuildConfig.FABRIC_LOADER_VERSION}")

    fabricModule("fabric-block-getter-api-v2")
    fabricModule("fabric-renderer-api-v1")

    implementation("net.caffeinemc:sodium-fabric:${BuildConfig.SODIUM_VERSION}")

    annotationProcessor("io.github.llamalad7:mixinextras-common:0.5.0")
    compileOnly("io.github.llamalad7:mixinextras-common:0.5.0")

    compileOnly("org.jspecify:jspecify:1.0.0")
    compileOnly("org.checkerframework:checker-qual:3.48.4")
}

loom {
    //accessWidenerPath = file("src/main/resources/betterblockentities-common.accesswidener")
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