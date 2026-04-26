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
        private const val LEAF_RADIUS = 4
        private const val LEAF_VERTICAL = 3
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
            val brokenLogs = SweepUtil.sweepConnected(
                block, player, SweepUtil.LOG_TYPES, MAX_LOGS,
                damageTool = false, faceOnly = true
            )
            clearLeavesNearLogs(block, brokenLogs, player)
        } catch (t: Throwable) {
            player.server.logger.warning("TreeSweeper error at ${block.location}: ${t.message}")
        } finally {
            SweepUtil.IN_SWEEP.set(false)
        }
    }

    private fun clearLeavesNearLogs(origin: Block, brokenLogs: List<Block>, player: org.bukkit.entity.Player) {
        val world = origin.world
        val tool = player.inventory.itemInMainHand
        val visited = HashSet<Long>()
        var broken = 0

        val centers = ArrayList<Block>(brokenLogs.size + 1)
        centers.add(origin)
        centers.addAll(brokenLogs)

        for (center in centers) {
            if (broken >= MAX_LEAVES) return
            for (dx in -LEAF_RADIUS..LEAF_RADIUS) {
                for (dy in -LEAF_VERTICAL..LEAF_VERTICAL) {
                    for (dz in -LEAF_RADIUS..LEAF_RADIUS) {
                        if (broken >= MAX_LEAVES) return
                        val nx = center.x + dx; val ny = center.y + dy; val nz = center.z + dz
                        if (!visited.add(SweepUtil.blockKey(nx, ny, nz))) continue
                        if (!world.isChunkLoaded(nx shr 4, nz shr 4)) continue
                        val leaf = world.getBlockAt(nx, ny, nz)
                        if (leaf.type !in SweepUtil.LEAF_TYPES) continue

                        val drops = try { leaf.getDrops(tool) } catch (_: Throwable) { continue }
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

}
