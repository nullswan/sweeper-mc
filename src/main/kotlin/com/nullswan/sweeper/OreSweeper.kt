package com.nullswan.sweeper

import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.plugin.Plugin

class OreSweeper(private val plugin: Plugin, private val debug: Boolean) : Listener {

    companion object {
        private const val MAX_ORES = 32
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onBreak(event: BlockBreakEvent) {
        if (SweepUtil.IN_SWEEP.get()) return

        val block = event.block
        if (block.type !in SweepUtil.ORE_TYPES) return

        val player = event.player
        val tool = player.inventory.itemInMainHand
        if (tool.type !in SweepUtil.PICKAXE_TYPES) return

        SweepUtil.IN_SWEEP.set(true)
        try {
            val broken = SweepUtil.sweepConnected(block, player, SweepUtil.ORE_TYPES, MAX_ORES)
            if (debug) plugin.logger.info("[Sweep] ore ${block.type} @${block.location.toVector()} tool=${tool.type} broken=${broken.size}/${MAX_ORES}")
        } catch (t: Throwable) {
            plugin.logger.warning("OreSweeper error at ${block.location}: ${t.message}")
        } finally {
            SweepUtil.IN_SWEEP.set(false)
        }
    }
}
