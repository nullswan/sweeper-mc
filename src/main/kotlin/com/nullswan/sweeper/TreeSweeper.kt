package com.nullswan.sweeper

import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent

class TreeSweeper : Listener {

    companion object {
        private const val MAX_LOGS = 48
        private const val MAX_LEAVES = 400
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onBreak(event: BlockBreakEvent) {
        if (SweepUtil.IN_SWEEP.get()) return

        val block = event.block
        if (block.type !in SweepUtil.LOG_TYPES) return

        val player = event.player
        val tool = player.inventory.itemInMainHand
        if (tool.type !in SweepUtil.AXE_TYPES) return
        if (!SweepUtil.toolHasDurability(tool)) return

        SweepUtil.IN_SWEEP.set(true)
        try {
            val brokenLogs = SweepUtil.sweepConnected(block, player, SweepUtil.LOG_TYPES, MAX_LOGS)
            clearNearbyLeaves(block, brokenLogs, player)
        } catch (t: Throwable) {
            player.server.logger.warning("TreeSweeper error at ${block.location}: ${t.message}")
        } finally {
            SweepUtil.IN_SWEEP.set(false)
        }
    }

    private fun clearNearbyLeaves(origin: Block, brokenLogs: List<Block>, player: org.bukkit.entity.Player) {
        var minX = origin.x; var maxX = origin.x
        var minY = origin.y; var maxY = origin.y
        var minZ = origin.z; var maxZ = origin.z
        for (log in brokenLogs) {
            minX = minOf(minX, log.x); maxX = maxOf(maxX, log.x)
            minY = minOf(minY, log.y); maxY = maxOf(maxY, log.y)
            minZ = minOf(minZ, log.z); maxZ = maxOf(maxZ, log.z)
        }

        val world = origin.world
        val tool = player.inventory.itemInMainHand
        var broken = 0
        for (x in (minX - 6)..(maxX + 6)) {
            for (y in (minY - 2)..(maxY + 4)) {
                for (z in (minZ - 6)..(maxZ + 6)) {
                    if (broken >= MAX_LEAVES) return
                    if (!world.isChunkLoaded(x shr 4, z shr 4)) continue
                    val leaf = world.getBlockAt(x, y, z)
                    if (leaf.type !in SweepUtil.LEAF_TYPES) continue

                    val drops = try {
                        leaf.getDrops(tool)
                    } catch (_: Throwable) {
                        continue
                    }
                    leaf.type = Material.AIR
                    broken++
                    for (drop in drops) {
                        val leftover = player.inventory.addItem(drop)
                        for (remaining in leftover.values) {
                            world.dropItemNaturally(leaf.location, remaining)
                        }
                    }
                }
            }
        }
    }
}
