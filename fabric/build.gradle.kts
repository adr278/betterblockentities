import net.fabricmc.loom.task.RemapJarTask
import org.gradle.jvm.tasks.Jar
import org.gradle.api.file.DuplicatesStrategy

plugins {
    id("java-library")

    // loom
    id("fabric-loom") version BuildConfig.LOOM_VERSION

    // local build-source plugins
    id("build-base")
    id("build-publish")
}

//set "main" artifact name, we append the version string received via the "build-base" plugin later
base {
    archivesName.set("bbe-fabric")
}

fun createResolvableConfiguration(name: String): Configuration = configurations.create(name) {
    isCanBeResolved = true
    isCanBeConsumed = false
}

val configurationApiModJava = createResolvableConfiguration("apiJava")
val configurationCommonModJava = createResolvableConfiguration("commonJava")
val configurationApiModSources = createResolvableConfiguration("apiSources")
val configurationCommonModResources = createResolvableConfiguration("commonResources")

dependencies {
    configurationCommonModJava(project(path = ":common", configuration = "commonMainJava"))
    configurationApiModJava(project(path = ":common", configuration = "commonApiJava"))

    configurationApiModSources(project(path = ":common", configuration = "commonApiSources"))

    configurationCommonModResources(project(path = ":common", configuration = "commonMainResources"))
    configurationCommonModResources(project(path = ":common", configuration = "commonApiResources"))
}

sourceSets.apply {
    main {
        compileClasspath += configurationCommonModJava
        compileClasspath += configurationApiModJava
        runtimeClasspath += configurationCommonModJava
        runtimeClasspath += configurationApiModJava
        runtimeClasspath += configurationCommonModResources
    }
}

loom {
    accessWidenerPath = file("src/main/resources/betterblockentities-fabric.accesswidener")

    mods {
        create("betterblockentities") {
            sourceSet(sourceSets["main"])
            sourceSet(project(":common").sourceSets["main"])
            sourceSet(project(":common").sourceSets["api"])
        }
    }
}

//helper function for including a fabric api module as compileOnly
fun DependencyHandlerScope.fabricModule(name: String) {
    compileOnly(fabricApi.module(name, BuildConfig.FABRIC_API_VERSION))
}

//declare maven repositories
repositories {
    maven("https://maven.fabricmc.net/")
    maven("https://maven.caffeinemc.net/releases")
    maven("https://maven.caffeinemc.net/snapshots")

    mavenCentral()
}

//specify version specific dependencies from declared maven repositories
dependencies {
    minecraft("com.mojang:minecraft:${BuildConfig.MINECRAFT_VERSION}")
    mappings(loom.officialMojangMappings())

    modImplementation("net.fabricmc:fabric-loader:${BuildConfig.FABRIC_LOADER_VERSION}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${BuildConfig.FABRIC_API_VERSION}")
    modImplementation("net.caffeinemc:sodium-fabric:${BuildConfig.SODIUM_VERSION}")

    compileOnly("net.fabricmc:sponge-mixin:0.13.2+mixin.0.8.5")
    compileOnly("io.github.llamalad7:mixinextras-common:0.5.4")

    fabricModule("fabric-renderer-api-v1")

    compileOnly("org.jspecify:jspecify:1.0.0")

    annotationProcessor("io.github.llamalad7:mixinextras-common:0.5.4")
}

tasks {
    val modOutputDir = rootProject.layout.buildDirectory.dir("mod")
    val apiOutputDir = rootProject.layout.buildDirectory.dir("api")
    val licenseFile = rootProject.file("LICENSE")
    val mainSourceSet = sourceSets.named("main")

    named<Jar>("jar") {
        archiveBaseName.set(base.archivesName)
        archiveVersion.set(project.version.toString())
        archiveClassifier.set("dev")
        destinationDirectory.set(layout.buildDirectory.dir("devlibs"))

        from(mainSourceSet.map { it.output })
        from(configurationCommonModJava)
        from(configurationApiModJava)
        from(configurationCommonModResources)
        from(licenseFile)

        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    }

    named<RemapJarTask>("remapJar") {
        archiveBaseName.set(base.archivesName)
        archiveVersion.set(project.version.toString())
        archiveClassifier.set("")
        destinationDirectory.set(modOutputDir)

        dependsOn(named("jar"))
    }

    register<Jar>("apiJar") {
        archiveBaseName.set(base.archivesName)
        archiveVersion.set(project.version.toString())
        archiveClassifier.set("api")
        destinationDirectory.set(apiOutputDir)

        from(configurationApiModJava)
        from(licenseFile)

        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    }

    register<Jar>("apiSourcesJar") {
        archiveBaseName.set(base.archivesName)
        archiveVersion.set(project.version.toString())
        archiveClassifier.set("api-sources")
        destinationDirectory.set(apiOutputDir)

        from(configurationApiModSources)
        from(licenseFile)

        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    }

    named("assemble") {
        dependsOn(named("remapJar"))
        dependsOn(named("apiJar"))
        dependsOn(named("apiSourcesJar"))
    }
}

artifacts {
    add("archives", tasks.named("remapJar"))
    add("archives", tasks.named("apiJar"))
    add("archives", tasks.named("apiSourcesJar"))
}

//maven publishing - maven setup/declaration happens in the "build-publish" plugin
publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = project.group.toString()
            artifactId = base.archivesName.get()
            version = project.version.toString()

            artifact(tasks.named("remapJar"))
        }

        create<MavenPublication>("mavenApi") {
            groupId = project.group.toString()
            artifactId = base.archivesName.get() + "-api"
            version = project.version.toString()

            artifact(tasks.named("apiJar")) {
                classifier = null
            }

            artifact(tasks.named("apiSourcesJar")) {
                classifier = "sources"
            }
        }
    }
}