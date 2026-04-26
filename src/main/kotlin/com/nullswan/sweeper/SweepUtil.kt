package com.nullswan.sweeper

import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

object SweepUtil {

    val IN_SWEEP = ThreadLocal.withInitial { false }

    val LOG_TYPES = setOf(
        Material.OAK_LOG, Material.SPRUCE_LOG, Material.BIRCH_LOG,
        Material.JUNGLE_LOG, Material.ACACIA_LOG, Material.DARK_OAK_LOG,
        Material.MANGROVE_LOG, Material.CHERRY_LOG,
        Material.CRIMSON_STEM, Material.WARPED_STEM,
        Material.PALE_OAK_LOG
    )

    val ORE_TYPES = setOf(
        Material.COAL_ORE, Material.DEEPSLATE_COAL_ORE,
        Material.IRON_ORE, Material.DEEPSLATE_IRON_ORE,
        Material.GOLD_ORE, Material.DEEPSLATE_GOLD_ORE,
        Material.DIAMOND_ORE, Material.DEEPSLATE_DIAMOND_ORE,
        Material.EMERALD_ORE, Material.DEEPSLATE_EMERALD_ORE,
        Material.LAPIS_ORE, Material.DEEPSLATE_LAPIS_ORE,
        Material.REDSTONE_ORE, Material.DEEPSLATE_REDSTONE_ORE,
        Material.COPPER_ORE, Material.DEEPSLATE_COPPER_ORE,
        Material.NETHER_GOLD_ORE, Material.NETHER_QUARTZ_ORE,
        Material.ANCIENT_DEBRIS
    )

    val CROP_TYPES = setOf(
        Material.WHEAT, Material.CARROTS, Material.POTATOES,
        Material.BEETROOTS, Material.NETHER_WART
    )

    val AXE_TYPES = setOf(
        Material.WOODEN_AXE, Material.STONE_AXE, Material.IRON_AXE,
        Material.GOLDEN_AXE, Material.DIAMOND_AXE, Material.NETHERITE_AXE
    )

    val PICKAXE_TYPES = setOf(
        Material.WOODEN_PICKAXE, Material.STONE_PICKAXE, Material.IRON_PICKAXE,
        Material.GOLDEN_PICKAXE, Material.DIAMOND_PICKAXE, Material.NETHERITE_PICKAXE
    )

    val HOE_TYPES = setOf(
        Material.WOODEN_HOE, Material.STONE_HOE, Material.IRON_HOE,
        Material.GOLDEN_HOE, Material.DIAMOND_HOE, Material.NETHERITE_HOE
    )

    val LEAF_TYPES = setOf(
        Material.OAK_LEAVES, Material.SPRUCE_LEAVES, Material.BIRCH_LEAVES,
        Material.JUNGLE_LEAVES, Material.ACACIA_LEAVES, Material.DARK_OAK_LEAVES,
        Material.MANGROVE_LEAVES, Material.CHERRY_LEAVES, Material.AZALEA_LEAVES,
        Material.FLOWERING_AZALEA_LEAVES, Material.PALE_OAK_LEAVES
    )

    fun sweepConnected(
        start: Block, player: Player, match: Set<Material>, maxBlocks: Int,
        damageTool: Boolean = true, faceOnly: Boolean = false
    ): List<Block> {
        val type = start.type
        val visited = HashSet<Long>()
        visited.add(blockKey(start))
        val broken = mutableListOf<Block>()
        val tool = player.inventory.itemInMainHand

        val queue = ArrayDeque<Block>()
        addMatchingNeighbors(start, type, visited, queue, match, faceOnly)

        while (queue.isNotEmpty() && broken.size < maxBlocks) {
            val b = queue.removeFirst()
            if (b.type != type) continue
            if (!b.world.isChunkLoaded(b.x shr 4, b.z shr 4)) continue

            if (damageTool) {
                if (!player.breakBlock(b)) break
            } else {
                breakAndGive(b, tool, player)
            }
            broken.add(b)

            addMatchingNeighbors(b, type, visited, queue, match, faceOnly)
        }
        return broken
    }

    fun breakAndGive(block: Block, tool: ItemStack, player: Player) {
        val drops = try { block.getDrops(tool) } catch (_: Throwable) { emptyList() }
        block.type = Material.AIR
        for (drop in drops) {
            val leftover = player.inventory.addItem(drop)
            for (remaining in leftover.values) {
                block.world.dropItemNaturally(block.location, remaining)
            }
        }
    }

    private val FACE_OFFSETS = listOf(
        intArrayOf(-1, 0, 0), intArrayOf(1, 0, 0),
        intArrayOf(0, -1, 0), intArrayOf(0, 1, 0),
        intArrayOf(0, 0, -1), intArrayOf(0, 0, 1),
    )

    private fun addMatchingNeighbors(
        block: Block, type: Material,
        visited: HashSet<Long>, queue: ArrayDeque<Block>,
        match: Set<Material>, faceOnly: Boolean
    ) {
        val world = block.world
        val x = block.x; val y = block.y; val z = block.z
        if (faceOnly) {
            for (off in FACE_OFFSETS) {
                val nx = x + off[0]; val ny = y + off[1]; val nz = z + off[2]
                if (!world.isChunkLoaded(nx shr 4, nz shr 4)) continue
                if (!visited.add(blockKey(nx, ny, nz))) continue
                val neighbor = world.getBlockAt(nx, ny, nz)
                if (neighbor.type in match) queue.add(neighbor)
            }
        } else {
            for (dx in -1..1) for (dy in -1..1) for (dz in -1..1) {
                if (dx == 0 && dy == 0 && dz == 0) continue
                val nx = x + dx; val ny = y + dy; val nz = z + dz
                if (!world.isChunkLoaded(nx shr 4, nz shr 4)) continue
                if (!visited.add(blockKey(nx, ny, nz))) continue
                val neighbor = world.getBlockAt(nx, ny, nz)
                if (neighbor.type in match) queue.add(neighbor)
            }
        }
    }

    internal fun blockKey(b: Block): Long = blockKey(b.x, b.y, b.z)

    internal fun blockKey(x: Int, y: Int, z: Int): Long =
        (x.toLong() and 0x3FFFFFF) or
            ((y.toLong() and 0xFFF) shl 26) or
            ((z.toLong() and 0x3FFFFFF) shl 38)
}
