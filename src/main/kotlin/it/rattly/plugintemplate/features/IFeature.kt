package it.rattly.plugintemplate.features

import org.bukkit.event.Listener

abstract class IFeature : Listener {
    open fun onEnable() {}
    open fun onDisable() {}
}