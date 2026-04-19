plugins {
    java
    id("com.gradleup.shadow") version "8.3.5"
    id("xyz.jpenilla.run-paper") version "2.3.1"
}

group = "dev.brmz"
version = "1.0.0-SNAPSHOT"
description = "MarketHub — Mercado híbrido estilo Grand Exchange para Minecraft"

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://jitpack.io")
    maven("https://maven.citizensnpcs.co/repo")
    maven("https://repo.opencollab.dev/main/") // Geyser / Floodgate
}

dependencies {
    // Paper API
    compileOnly("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")

    // Vault
    compileOnly("com.github.MilkBowl:VaultAPI:1.7.1")

    // Citizens 2
    compileOnly("net.citizensnpcs:citizens-main:2.0.35-SNAPSHOT") {
        exclude(group = "*", module = "*")
    }

    // Floodgate API (Bedrock detection)
    compileOnly("org.geysermc.floodgate:api:2.2.3-SNAPSHOT")

    // --- Shaded dependencies ---

    // TriumphGUI (chest inventory UI)
    implementation("dev.triumphteam:triumph-gui:3.1.13")

    // HikariCP (connection pool)
    implementation("com.zaxxer:HikariCP:6.2.1")

    // SQLite JDBC (padrão)
    implementation("org.xerial:sqlite-jdbc:3.47.1.0")

    // MySQL Connector (opcional, para quem usa MySQL)
    implementation("com.mysql:mysql-connector-j:9.1.0")
}

tasks {
    shadowJar {
        archiveClassifier.set("")

        // Relocate shaded libs to avoid conflicts with other plugins
        relocate("dev.triumphteam.gui", "dev.brmz.markethub.lib.gui")
        relocate("com.zaxxer.hikari", "dev.brmz.markethub.lib.hikari")
        relocate("com.mysql", "dev.brmz.markethub.lib.mysql")
    }

    build {
        dependsOn(shadowJar)
    }

    processResources {
        val props = mapOf(
            "version" to project.version,
            "description" to project.description
        )
        inputs.properties(props)
        filesMatching("plugin.yml") {
            expand(props)
        }
    }

    runServer {
        minecraftVersion("1.21.4")
    }

    compileJava {
        options.encoding = "UTF-8"
    }
}
