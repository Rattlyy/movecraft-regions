package it.rattly.regions.commands

import org.bukkit.event.Listener

abstract class ICommand : Listener {
    abstract fun define()
}