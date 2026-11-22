plugins {
    `java-library`
    `maven-publish`
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
    implementation(libs.kff) {
        // ローダーは対象のNeoForgeが供給し、KFFの版範囲から別バージョンを解決しない。
        exclude(group = "net.neoforged.fancymodloader", module = "loader")
    }
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
            // JEIは試遊専用。専用サーバー・datagen・GameTestへ持ち込まない。
            additionalRuntimeClasspathConfiguration.dependencies.add(
                project.dependencies.create("maven.modrinth:jei:ufHUqt9b"),
            )
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

val verifyModJar =
    tasks.register<VerifyModJarTask>("verifyModJar") {
        modJar.set(tasks.jar.flatMap { it.archiveFile })
        modId.set(zincModId)
        val path = providers.gradleProperty("mod_package").get().replace('.', '/')
        packagePath.set(path)
        val items = listOf("dust_zinc", "dirty_dust_zinc", "clump_zinc", "shard_zinc", "crystal_zinc")
        val categories = listOf("dusts", "dirty_dusts", "clumps", "shards", "crystals")
        val recipes =
            listOf(
                "clump/from_ore",
                "clump/from_raw_block",
                "clump/from_raw_ore",
                "clump/from_shard",
                "crystal/from_slurry",
                "dirty_dust/from_clump",
                "dust/from_dirty_dust",
                "dust/from_ingot",
                "dust/from_ore",
                "dust/from_raw_block",
                "dust/from_raw_ore",
                "ingot/from_dust_blasting",
                "ingot/from_dust_smelting",
                "shard/from_crystal",
                "shard/from_ore",
                "shard/from_raw_block",
                "shard/from_raw_ore",
                "slurry/clean",
                "slurry/dirty/from_ore",
                "slurry/dirty/from_raw_block",
                "slurry/dirty/from_raw_ore",
            )
        requiredEntries.set(
            listOf(
                "$path/content/ZincContent.class",
                "$path/processing/ZincProcessing.class",
                "$path/CreateMekanismZinc.class",
                "$path/ZincRegistries.class",
                "$path/ZincDataGenerators.class",
                "META-INF/neoforge.mods.toml",
                "META-INF/LICENSE",
                "assets/$zincModId/lang/en_us.json",
                "assets/$zincModId/lang/ja_jp.json",
                "data/mekanism/data_maps/mekanism/chemical/chemical_solid_tag.json",
            ) +
                items.flatMap {
                    listOf(
                        "assets/$zincModId/textures/item/$it.png",
                        "assets/$zincModId/models/item/$it.json",
                    )
                } +
                categories.flatMap { listOf("data/c/tags/item/$it.json", "data/c/tags/item/$it/zinc.json") } +
                recipes.map { "data/$zincModId/recipe/processing/zinc/$it.json" },
        )
    }
tasks.named("build") { dependsOn(verifyModJar) }

tasks.named("check") { dependsOn("detektGametest") }

publishing {
    publications {
        create<MavenPublication>("mod") {
            artifactId = base.archivesName.get()
            artifact(tasks.jar)
            pom {
                name.set(providers.gradleProperty("mod_name"))
                description.set(providers.gradleProperty("mod_description"))
                url.set(providers.gradleProperty("mod_repo_url"))
            }
        }
    }
    val githubRepository = providers.environmentVariable("GITHUB_REPOSITORY")
    if (githubRepository.isPresent) {
        repositories {
            maven {
                name = "githubPackages"
                url = uri("https://maven.pkg.github.com/${githubRepository.get().lowercase()}")
                credentials(PasswordCredentials::class)
            }
        }
    }
}
