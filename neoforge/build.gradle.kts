import org.gradle.api.file.DuplicatesStrategy
import org.gradle.jvm.tasks.Jar

plugins {
    id("java-library")

    id("net.neoforged.moddev") version BuildConfig.MODDEV_GRADLE_VERSION

    id("build-base")
    id("build-publish")
}

repositories {
    maven("https://maven.su5ed.dev/releases")
    maven("https://maven.neoforged.net/releases/")
    maven("https://maven.fabricmc.net/")
    maven("https://maven.caffeinemc.net/releases")
    maven("https://maven.caffeinemc.net/snapshots")
    mavenCentral()
}

base {
    archivesName.set("bbe-neoforge")
}

fun createResolvableConfiguration(name: String): Configuration = configurations.create(name) {
    isCanBeResolved = true
    isCanBeConsumed = false
}

val configurationApiModJava = createResolvableConfiguration("apiJava")
val configurationCommonModJava = createResolvableConfiguration("commonJava")
val configurationApiModSources = createResolvableConfiguration("apiSources")
val configurationCommonModResources = createResolvableConfiguration("commonResources")

val sodiumNeoForge: Configuration = configurations.create("sodiumNeoForge") {
    isCanBeResolved = false
    isCanBeConsumed = false
}

configurations {
    compileOnly {
        extendsFrom(sodiumNeoForge)
    }
}

dependencies {
    configurationCommonModJava(project(path = ":common", configuration = "commonMainJava"))
    configurationApiModJava(project(path = ":common", configuration = "commonApiJava"))
    configurationApiModSources(project(path = ":common", configuration = "commonApiSources"))
    configurationCommonModResources(project(path = ":common", configuration = "commonMainResources"))
    configurationCommonModResources(project(path = ":common", configuration = "commonApiResources"))

    sodiumNeoForge("net.caffeinemc:sodium-neoforge:${BuildConfig.SODIUM_VERSION}") {
        isTransitive = false
    }
}

sourceSets {
    named("main") {
        compileClasspath += configurationCommonModJava
        compileClasspath += configurationApiModJava
        runtimeClasspath += configurationCommonModJava
        runtimeClasspath += configurationApiModJava
    }
}

tasks {
    //these become :
    // "rootDir/build/mod"
    // "rootDir/build/api"
    val modOutputDir = rootProject.layout.buildDirectory.dir("mod")
    val apiOutputDir = rootProject.layout.buildDirectory.dir("api")

    //
    val mainSourceSet = sourceSets.named("main")
    val licenseFile = rootProject.file("LICENSE")

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

    register<Jar>("modJar") {
        archiveBaseName.set(base.archivesName)
        archiveVersion.set(project.version.toString())
        archiveClassifier.set("")
        destinationDirectory.set(modOutputDir)

        from(mainSourceSet.map { it.output })
        from(configurationCommonModJava)
        from(configurationApiModJava)
        from(configurationCommonModResources)
        from(licenseFile)

        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
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
        dependsOn(named("modJar"))
        dependsOn(named("apiJar"))
        dependsOn(named("apiSourcesJar"))
    }
}

artifacts {
    add("archives", tasks.named("modJar"))
    add("archives", tasks.named("apiJar"))
    add("archives", tasks.named("apiSourcesJar"))
}

neoForge {
    version = BuildConfig.NEOFORGE_VERSION

    parchment {
        minecraftVersion = BuildConfig.MINECRAFT_VERSION
        mappingsVersion = BuildConfig.PARCHMENT_VERSION
    }

    mods {
        create("betterblockentities") {
            sourceSet(sourceSets["main"])
            sourceSet(project(":common").sourceSets["main"])
            sourceSet(project(":common").sourceSets["api"])
        }
    }

    runs {
        create("Client") {
            client()
            ideName = "NeoForge/Client"
        }
    }
}

configurations.named("additionalRuntimeClasspath") {
    extendsFrom(sodiumNeoForge)
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = project.group.toString()
            artifactId = base.archivesName.get()
            version = project.version.toString()
            artifact(tasks.named("modJar"))
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
