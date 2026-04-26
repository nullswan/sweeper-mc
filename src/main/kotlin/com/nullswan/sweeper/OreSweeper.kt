package com.nullswan.sweeper

import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent

class OreSweeper : Listener {

    companion object {
        private const val MAX_ORES = 32
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onBreak(event: BlockBreakEvent) {
        if (SweepUtil.IN_SWEEP.get()) return

        val block = event.block
        if (block.type !in SweepUtil.ORE_TYPES) return

        val tool = event.player.inventory.itemInMainHand
        if (tool.type !in SweepUtil.PICKAXE_TYPES) return

        SweepUtil.IN_SWEEP.set(true)
        try {
            SweepUtil.sweepConnected(block, event.player, SweepUtil.ORE_TYPES, MAX_ORES)
        } finally {
            SweepUtil.IN_SWEEP.set(false)
        }
    }
}
