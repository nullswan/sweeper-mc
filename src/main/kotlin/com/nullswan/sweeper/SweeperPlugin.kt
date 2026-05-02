package com.nullswan.sweeper

import org.bukkit.plugin.java.JavaPlugin

class SweeperPlugin : JavaPlugin() {

    override fun onEnable() {
        saveDefaultConfig()
        val debug = config.getBoolean("debug", false)
        val pm = server.pluginManager
        pm.registerEvents(TreeSweeper(this, debug), this)
        pm.registerEvents(OreSweeper(this, debug), this)
        pm.registerEvents(CropSweeper(this, debug), this)
        logger.info("Sweeper enabled — trees, ores, and crops (debug=$debug)")
    }
}
