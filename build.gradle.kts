import io.papermc.paperweight.util.path
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLDecoder
import java.nio.file.Files
import java.security.MessageDigest

plugins {
    kotlin("jvm") version "2.0.0"

    id("io.papermc.paperweight.userdev") version "1.7.1"
    id("net.minecrell.plugin-yml.paper") version "0.6.0"
}

val mainClassName = "MovecraftRegions"
group = "it.rattly"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()

    maven("https://repo.papermc.io/repository/maven-public")
    maven("https://repo.codemc.org/repository/maven-public/")
    maven("https://maven.enginehub.org/repo/")
    maven("https://jitpack.io")
}

dependencies {
    paperweight.paperDevBundle("1.20.5-R0.1-SNAPSHOT")

    library(kotlin("stdlib"))
    library(kotlin("reflect"))
    library("io.github.classgraph:classgraph:4.8.165")

    library("net.axay:kspigot:1.20.3") {
        exclude("org.bukkit", "bukkit")
    }

    library("de.hglabor.utils:kutils:1.0.0-beta") {
        exclude("org.bukkit", "bukkit")
    }

    compileOnly("dev.jorel:commandapi-bukkit-core:9.3.0")
    compileOnly("dev.jorel:commandapi-bukkit-kotlin:9.3.0")
}

paper {
    apiVersion = "1.20"
    main = "$group.${rootProject.name}.$mainClassName"
    bootstrapper = "$group.${rootProject.name}.paper.PaperBootstrapper"
    loader = "$group.${rootProject.name}.paper.PaperLoader"
    hasOpenClassloader = true
    generateLibrariesJson = true
    serverDependencies {
        register("CommandAPI")
    }
}

kotlin {
    jvmToolchain(21)
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

tasks {
    jar {
        archiveFileName.set("$mainClassName-noRemap.jar")
    }

    reobfJar {
        dependsOn(jar)
        inputJar.set(jar.get().archiveFile.get())
        outputJar.set(file("${layout.buildDirectory.path}/libs/$mainClassName.jar"))
    }

    build {
        dependsOn(reobfJar)
    }

    create("setupTestServer") {
        mustRunAfter("build")
        doFirst {
            val folder = file(layout.projectDirectory.path.toString() + "/server/").also { it.mkdirs() }

            // credits: horizon's end ion
            fun downloadJenkinsArtifact(domain: String, project: String, filter: String, location: String, file: File) {
                val jarName =
                    URL("https://$domain/job/$project/lastSuccessfulBuild/api/xml?xpath=/freeStyleBuild/artifact/relativePath${if (filter.isNotEmpty()) "[$filter]" else ""}")
                        .readText()
                        .substringAfter("<relativePath>$location/")
                        .substringBefore("</relativePath>")

                file.writeBytes(
                    URL("https://$domain/job/$project/lastSuccessfulBuild/artifact/$location/$jarName")
                        .readBytes()
                )
            }

            // credits: random stackoverflow guy for handling redirects
            fun download(stringUrl: String, file: File) {
                val visited: MutableMap<String, Int> = mutableMapOf()
                var url = stringUrl

                repeat(3) {
                    val times = visited.getOrDefault(url, 0) + 1
                    visited[url] = times

                    val conn = URL(url).openConnection() as HttpURLConnection
                    conn.apply {
                        connectTimeout = 15000
                        readTimeout = 15000
                        instanceFollowRedirects = false
                        setRequestProperty("User-Agent", "RattlyDownloader") // as asked by spiget
                    }

                    when (conn.responseCode) {
                        HttpURLConnection.HTTP_MOVED_PERM, HttpURLConnection.HTTP_MOVED_TEMP -> {
                            val location = URLDecoder.decode(conn.getHeaderField("Location"), "UTF-8")
                            val base = URL(url)
                            val next = URL(base, location) // Deal with relative URLs
                            url = next.toExternalForm()
                        }

                        else -> {
                            conn.inputStream.use { it.copyTo(FileOutputStream(file)) }
                            return@download
                        }
                    }
                }
            }

            fun hashAndDownload(fileName: String, isPlugin: Boolean = true, downloadCallback: (File) -> Unit) {
                val file = File(folder, "/${if (isPlugin) "plugins/" else ""}$fileName.jar")

                if (file.exists()) {
                    val hash = MessageDigest.getInstance("MD5").digest(Files.readAllBytes(file.toPath()))
                    val hashFile = File(folder, "/hash/${fileName}Hash")

                    if (!hashFile.exists()) {
                        downloadCallback(file)
                        hashFile.writeBytes(hash)
                    } else {
                        if (!hash.contentEquals(hashFile.readBytes())) {
                            logger.info("Downloading $fileName...")
                            downloadCallback(file)
                        }
                    }
                } else {
                    downloadCallback(file)
                    val hash = MessageDigest.getInstance("MD5").digest(Files.readAllBytes(file.toPath()))
                    val hashFile = File(folder, "/hash/${fileName}Hash")

                    hashFile.writeBytes(hash)
                }
            }

            hashAndDownload(
                "server",
                false
            ) {
                downloadJenkinsArtifact("ci.pufferfish.host", "Pufferfish-1.20", "contains(., 'reobf')", "build/libs", it)
            }

            hashAndDownload(
                "displayEditor",
            ) {
                download(
                    "https://api.spiget.org/v2/resources/113254/download",
                    it
                )
            }

            hashAndDownload(
                "itemEdit",
            ) {
                download(
                    "https://api.spiget.org/v2/resources/40993/download",
                    it
                )
            }

            hashAndDownload(
                "fawe",
            ) {
                downloadJenkinsArtifact("ci.athion.net", "FastAsyncWorldEdit", "contains(.,'Bukkit')", "artifacts", it)
            }

            hashAndDownload(
                "luckPerms",
            ) {
                downloadJenkinsArtifact(
                    "ci.lucko.me",
                    "LuckPerms",
                    "starts-with(.,'bukkit/')",
                    "bukkit/loader/build/libs",
                    it
                )
            }

            hashAndDownload(
                "essentials",
            ) {
                downloadJenkinsArtifact(
                    "ci.ender.zone",
                    "EssentialsX",
                    "contains(.,'EssentialsX-')",
                    "jars",
                    it
                )
            }

            Runtime.getRuntime()
                .exec(arrayOf("cp", "-r", reobfJar.get().outputJar.get().path.toString(), "server/plugins/"))
                .waitFor()
        }
    }
}