package com.nullswan.sweeper

import org.bukkit.plugin.java.JavaPlugin

class SweeperPlugin : JavaPlugin() {

    override fun onEnable() {
        val pm = server.pluginManager
        pm.registerEvents(TreeSweeper(), this)
        pm.registerEvents(OreSweeper(), this)
        pm.registerEvents(CropSweeper(), this)
        logger.info("Sweeper enabled — trees, ores, and crops")
    }
}
