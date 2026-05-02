package com.nullswan.sweeper

import org.bukkit.Material
import org.bukkit.block.data.Ageable
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.plugin.Plugin

class CropSweeper(private val plugin: Plugin, private val debug: Boolean) : Listener {

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
        var broken = 0
        var replanted = 0
        try {
            if (replant(block, cropType)) replanted++

            for (dx in -RADIUS..RADIUS) {
                for (dz in -RADIUS..RADIUS) {
                    val target = block.getRelative(dx, 0, dz)
                    if (target == block) continue
                    if (target.type != cropType) continue
                    if (!target.world.isChunkLoaded(target.x shr 4, target.z shr 4)) continue

                    val data = target.blockData as? Ageable ?: continue
                    if (data.age < data.maximumAge) continue

                    if (player.breakBlock(target)) {
                        broken++
                        if (replant(target, cropType)) replanted++
                    }
                }
            }
            if (debug) plugin.logger.info("[Sweep] crop $cropType @${block.location.toVector()} broken=$broken replanted=$replanted")
        } catch (t: Throwable) {
            plugin.logger.warning("CropSweeper error at ${block.location}: ${t.message}")
        } finally {
            SweepUtil.IN_SWEEP.set(false)
        }
    }

    private fun replant(block: org.bukkit.block.Block, cropType: Material): Boolean {
        val seedType = when (cropType) {
            Material.WHEAT -> Material.WHEAT
            Material.CARROTS -> Material.CARROTS
            Material.POTATOES -> Material.POTATOES
            Material.BEETROOTS -> Material.BEETROOTS
            Material.NETHER_WART -> Material.NETHER_WART
            else -> return false
        }
        val below = block.getRelative(0, -1, 0).type
        val soilOk = when (seedType) {
            Material.NETHER_WART -> below == Material.SOUL_SAND
            else -> below == Material.FARMLAND
        }
        if (!soilOk) return false
        block.type = seedType
        return true
    }
}
