plugins {
    `java-library`
    alias(libs.plugins.kotlin)
    alias(libs.plugins.moddev)
}
kotlin { jvmToolchain(21) }
val zincModId = providers.gradleProperty("mod_id").get()
base { archivesName.set("create-mekanism-zinc") }
repositories {
    maven("https://api.modrinth.com/maven") { content { includeGroup("maven.modrinth") } }
    maven("https://thedarkcolour.github.io/KotlinForForge/") { content { includeGroup("thedarkcolour") } }
}
val localRuntime = configurations.create("localRuntime")
dependencies {
    implementation(project(":core"))
    compileOnly("maven.modrinth:create:${property("create_version")}")
    compileOnly("maven.modrinth:mekanism:${property("mekanism_version")}")
    add("localRuntime", "maven.modrinth:create:${property("create_version")}")
    add("localRuntime", "maven.modrinth:mekanism:${property("mekanism_version")}")
    testImplementation(platform(libs.junit.bom))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("com.google.code.gson:gson:2.11.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
val metadata =
    tasks.register<ProcessResources>("generateModMetadata") {
        val values =
            listOf(
                "minecraft_version",
                "minecraft_version_range",
                "neo_version",
                "loader_version_range",
                "create_version_range",
                "mekanism_version_range",
                "mod_id",
                "mod_name",
                "mod_license",
                "mod_version",
                "mod_authors",
                "mod_description",
                "mod_repo_url",
            ).associateWith {
                providers
                    .gradleProperty(
                        it,
                    ).get()
                    .replace(
                        "\\",
                        "\\\\",
                    ).replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\t", "\\t")
            }
        inputs.properties(values)
        filteringCharset = "UTF-8"
        expand(values)
        from("src/main/templates")
        into(layout.buildDirectory.dir("generated/modMetadata"))
    }
sourceSets.main {
    resources.srcDir(metadata)
    resources.srcDir("src/generated/resources")
}
val gametest =
    sourceSets.create("gametest") {
        compileClasspath += sourceSets.main.get().output + sourceSets.main.get().compileClasspath
        runtimeClasspath += sourceSets.main.get().output + sourceSets.main.get().runtimeClasspath
    }
configurations[gametest.implementationConfigurationName].extendsFrom(configurations.implementation.get())
configurations[gametest.compileOnlyConfigurationName].extendsFrom(configurations.compileOnly.get())
configurations[gametest.runtimeOnlyConfigurationName].extendsFrom(configurations["localRuntime"])
configurations.runtimeClasspath { extendsFrom(configurations["localRuntime"]) }
val fixtureMetadata =
    tasks.register<ProcessResources>("generateGameTestModMetadata") {
        val values = mapOf("mod_version" to project.version.toString())
        inputs.properties(values)
        expand(values)
        from("src/gametest/templates")
        into(layout.buildDirectory.dir("generated/gameTestMetadata"))
    }
gametest.resources.srcDir(fixtureMetadata)
neoForge {
    version = providers.gradleProperty("neo_version").get()
    mods {
        create(zincModId) {
            sourceSet(sourceSets.main.get())
            sourceSet(project(":core").sourceSets.main.get())
        }
        create("${zincModId}_tests") { sourceSet(gametest) }
    }
    runs {
        create("client") {
            client()
            gameDirectory.set(rootProject.file(".local/client"))
        }
        create("server") {
            server()
            programArgument("--nogui")
            gameDirectory.set(rootProject.file(".local/server"))
        }
        create("data") {
            data()
            gameDirectory.set(rootProject.file(".local/data"))
            programArguments.addAll(
                "--mod",
                zincModId,
                "--all",
                "--output",
                file("src/generated/resources").absolutePath,
                "--existing",
                file("src/main/resources").absolutePath,
            )
        }
        create("gameTestServer") {
            type = "gameTestServer"
            gameDirectory.set(rootProject.file(".local/gametest"))
            systemProperty("neoforge.enabledGameTestNamespaces", zincModId)
        }
    }
    runs.configureEach { loadedMods.set(listOf(mods.getByName(zincModId))) }
    runs.getByName("gameTestServer").apply {
        sourceSet = gametest
        loadedMods.set(listOf(mods.getByName(zincModId), mods.getByName("${zincModId}_tests")))
    }
    ideSyncTask(metadata)
}
tasks.jar {
    from(
        project(":core")
            .sourceSets.main
            .get()
            .output,
    )
    from(rootProject.file("LICENSE")) { into("META-INF") }
    duplicatesStrategy = DuplicatesStrategy.FAIL
}
tasks.register<Jar>("gameTestJar") {
    archiveClassifier.set("gametests")
    from(gametest.output)
    dependsOn(tasks.named("gametestClasses"))
    duplicatesStrategy = DuplicatesStrategy.FAIL
}
tasks.processResources { exclude("**/.cache/**") }
tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(21)
}
tasks.test { useJUnitPlatform() }
