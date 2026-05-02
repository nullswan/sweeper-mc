package com.nullswan.sweeper

import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.plugin.Plugin

class TreeSweeper(private val plugin: Plugin, private val debug: Boolean) : Listener {

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

        SweepUtil.IN_SWEEP.set(true)
        try {
            val brokenLogs = SweepUtil.sweepConnected(
                block, player, SweepUtil.LOG_TYPES, MAX_LOGS,
                damageTool = false, faceOnly = true
            )
            val leavesBroken = clearLeavesNearLogs(block, brokenLogs, player)
            if (debug) plugin.logger.info("[Sweep] tree ${block.type} @${block.location.toVector()} tool=${tool.type} logs=${brokenLogs.size}/${MAX_LOGS} leaves=${leavesBroken}/${MAX_LEAVES}")
        } catch (t: Throwable) {
            plugin.logger.warning("TreeSweeper error at ${block.location}: ${t.message}")
        } finally {
            SweepUtil.IN_SWEEP.set(false)
        }
    }

    private fun clearLeavesNearLogs(origin: Block, brokenLogs: List<Block>, player: org.bukkit.entity.Player): Int {
        val world = origin.world
        val tool = player.inventory.itemInMainHand
        val visited = HashSet<Long>()
        var broken = 0

        val centers = ArrayList<Block>(brokenLogs.size + 1)
        centers.add(origin)
        centers.addAll(brokenLogs)

        for (center in centers) {
            if (broken >= MAX_LEAVES) return broken
            for (dx in -LEAF_RADIUS..LEAF_RADIUS) {
                for (dy in -LEAF_VERTICAL..LEAF_VERTICAL) {
                    for (dz in -LEAF_RADIUS..LEAF_RADIUS) {
                        if (broken >= MAX_LEAVES) return broken
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
        return broken
    }
}
