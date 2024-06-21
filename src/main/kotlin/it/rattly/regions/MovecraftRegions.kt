package it.rattly.regions

import dev.jorel.commandapi.CommandAPI
import it.rattly.regions.commands.ICommand
import it.rattly.regions.features.IFeature
import it.rattly.regions.utils.Command
import it.rattly.regions.utils.Feature
import it.rattly.regions.utils.cachedScan
import it.rattly.regions.utils.getAllAnnotatedWith
import net.axay.kspigot.main.KSpigot
import kotlin.system.measureTimeMillis

@Suppress("UnstableApiUsage")
object MovecraftRegions: KSpigot() {
    private val features by getAllAnnotatedWith<IFeature>(Feature::class)
    private val commands by getAllAnnotatedWith<ICommand>(Command::class)

    override fun startup() {
        val time = measureTimeMillis {
            features.forEach {
                logger.info { "Enabling feature ${it::class.simpleName}..." }
                server.pluginManager.registerEvents(it, this)
                it.onEnable()
            }

            commands.forEach {
                logger.info { "Enabling command ${it::class.simpleName}..." }
                server.pluginManager.registerEvents(it, this)
                it.define()
            }

            cachedScan.close()
        }

        logger.info { "${pluginMeta.name} enabled in ${time}ms!" }
    }

    override fun shutdown() {
        logger.info { "Disabling ${pluginMeta.name}..." }

        val time = measureTimeMillis {
            features.reversed().forEach {
                logger.info { "Disabling feature ${it::class.simpleName}..." }
                it.onDisable()
            }

            CommandAPI.getRegisteredCommands().forEach { CommandAPI.unregister(it.commandName) }
        }

        logger.info { "FlagPvP disabled in ${time}ms!" }
    }
}