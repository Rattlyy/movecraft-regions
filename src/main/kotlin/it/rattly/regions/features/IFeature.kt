package it.rattly.regions.features

import org.bukkit.event.Listener

abstract class IFeature : Listener {
    open fun onEnable() {}
    open fun onDisable() {}
}