package com.nullswan.sweeper

import org.bukkit.Material
import org.bukkit.block.data.Ageable
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent

class CropSweeper : Listener {

    companion object {
        private const val RADIUS = 4
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onBreak(event: BlockBreakEvent) {
        if (SweepUtil.IN_SWEEP.get()) return

        val block = event.block
        if (block.type !in SweepUtil.CROP_TYPES) return

        val ageable = block.blockData as? Ageable ?: return
        if (ageable.age < ageable.maximumAge) return

        val cropType = block.type
        val player = event.player

        SweepUtil.IN_SWEEP.set(true)
        try {
            for (dx in -RADIUS..RADIUS) {
                for (dz in -RADIUS..RADIUS) {
                    val target = block.getRelative(dx, 0, dz)
                    if (target == block) continue
                    if (target.type != cropType) continue

                    val data = target.blockData as? Ageable ?: continue
                    if (data.age < data.maximumAge) continue

                    if (player.breakBlock(target)) {
                        replant(target, cropType)
                    }
                }
            }
        } finally {
            SweepUtil.IN_SWEEP.set(false)
        }
    }

    private fun replant(block: org.bukkit.block.Block, cropType: Material) {
        val seedType = when (cropType) {
            Material.WHEAT -> Material.WHEAT
            Material.CARROTS -> Material.CARROTS
            Material.POTATOES -> Material.POTATOES
            Material.BEETROOTS -> Material.BEETROOTS
            Material.NETHER_WART -> Material.NETHER_WART
            else -> return
        }
        block.type = seedType
    }
}
