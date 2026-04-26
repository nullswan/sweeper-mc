package com.nullswan.sweeper

import org.bukkit.Material
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class SweepUtilTest {

    @Test
    fun `LOG_TYPES contains all overworld log types`() {
        assertTrue(Material.OAK_LOG in SweepUtil.LOG_TYPES)
        assertTrue(Material.SPRUCE_LOG in SweepUtil.LOG_TYPES)
        assertTrue(Material.BIRCH_LOG in SweepUtil.LOG_TYPES)
        assertTrue(Material.JUNGLE_LOG in SweepUtil.LOG_TYPES)
        assertTrue(Material.ACACIA_LOG in SweepUtil.LOG_TYPES)
        assertTrue(Material.DARK_OAK_LOG in SweepUtil.LOG_TYPES)
        assertTrue(Material.MANGROVE_LOG in SweepUtil.LOG_TYPES)
        assertTrue(Material.CHERRY_LOG in SweepUtil.LOG_TYPES)
        assertTrue(Material.PALE_OAK_LOG in SweepUtil.LOG_TYPES)
    }

    @Test
    fun `LOG_TYPES contains nether stems`() {
        assertTrue(Material.CRIMSON_STEM in SweepUtil.LOG_TYPES)
        assertTrue(Material.WARPED_STEM in SweepUtil.LOG_TYPES)
    }

    @Test
    fun `LOG_TYPES excludes planks and stripped logs`() {
        assertFalse(Material.OAK_PLANKS in SweepUtil.LOG_TYPES)
        assertFalse(Material.STRIPPED_OAK_LOG in SweepUtil.LOG_TYPES)
    }

    @Test
    fun `ORE_TYPES covers all vanilla ores and deepslate variants`() {
        val ores = listOf(
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
        for (ore in ores) {
            assertTrue(ore in SweepUtil.ORE_TYPES, "$ore should be in ORE_TYPES")
        }
    }

    @Test
    fun `ORE_TYPES excludes non-ore blocks`() {
        assertFalse(Material.STONE in SweepUtil.ORE_TYPES)
        assertFalse(Material.DEEPSLATE in SweepUtil.ORE_TYPES)
        assertFalse(Material.DIAMOND_BLOCK in SweepUtil.ORE_TYPES)
    }

    @Test
    fun `CROP_TYPES contains all harvestable crops`() {
        assertTrue(Material.WHEAT in SweepUtil.CROP_TYPES)
        assertTrue(Material.CARROTS in SweepUtil.CROP_TYPES)
        assertTrue(Material.POTATOES in SweepUtil.CROP_TYPES)
        assertTrue(Material.BEETROOTS in SweepUtil.CROP_TYPES)
        assertTrue(Material.NETHER_WART in SweepUtil.CROP_TYPES)
    }

    @Test
    fun `tool sets match correct tool tiers`() {
        val tiers = listOf("WOODEN", "STONE", "IRON", "GOLDEN", "DIAMOND", "NETHERITE")
        for (tier in tiers) {
            assertTrue(Material.valueOf("${tier}_AXE") in SweepUtil.AXE_TYPES)
            assertTrue(Material.valueOf("${tier}_PICKAXE") in SweepUtil.PICKAXE_TYPES)
            assertTrue(Material.valueOf("${tier}_HOE") in SweepUtil.HOE_TYPES)
        }
    }

    @Test
    fun `LEAF_TYPES contains all leaf variants`() {
        assertTrue(Material.OAK_LEAVES in SweepUtil.LEAF_TYPES)
        assertTrue(Material.CHERRY_LEAVES in SweepUtil.LEAF_TYPES)
        assertTrue(Material.AZALEA_LEAVES in SweepUtil.LEAF_TYPES)
        assertTrue(Material.FLOWERING_AZALEA_LEAVES in SweepUtil.LEAF_TYPES)
        assertTrue(Material.PALE_OAK_LEAVES in SweepUtil.LEAF_TYPES)
    }

    @Test
    fun `each tool set has exactly 6 tiers`() {
        assertEquals(6, SweepUtil.AXE_TYPES.size)
        assertEquals(6, SweepUtil.PICKAXE_TYPES.size)
        assertEquals(6, SweepUtil.HOE_TYPES.size)
    }

    // --- blockKey tests ---

    @Test
    fun `blockKey produces unique keys for adjacent coordinates`() {
        val keys = mutableSetOf<Long>()
        for (dx in -2..2) {
            for (dy in -2..2) {
                for (dz in -2..2) {
                    val key = SweepUtil.blockKey(100 + dx, 64 + dy, -200 + dz)
                    assertTrue(keys.add(key), "Collision at ($dx, $dy, $dz)")
                }
            }
        }
        assertEquals(125, keys.size)
    }

    @Test
    fun `blockKey handles negative coordinates`() {
        val k1 = SweepUtil.blockKey(-1, -64, -1)
        val k2 = SweepUtil.blockKey(0, 0, 0)
        val k3 = SweepUtil.blockKey(-1000, -64, -1000)
        assertTrue(k1 != k2, "Negative coords should differ from origin")
        assertTrue(k1 != k3, "Different negative coords should differ")
    }

    @Test
    fun `blockKey produces no collisions in large grid`() {
        val keys = HashSet<Long>(10000)
        for (x in -50..50) {
            for (y in -64..64) {
                for (z in -5..5) {
                    assertTrue(keys.add(SweepUtil.blockKey(x, y, z)),
                        "Collision at ($x, $y, $z)")
                }
            }
        }
    }

    @Test
    fun `blockKey handles extreme Y values`() {
        val kBottom = SweepUtil.blockKey(0, -64, 0)
        val kTop = SweepUtil.blockKey(0, 320, 0)
        assertTrue(kBottom != kTop, "Min and max Y should produce different keys")
    }

    // --- IN_SWEEP guard tests ---

    @Test
    fun `IN_SWEEP defaults to false`() {
        assertFalse(SweepUtil.IN_SWEEP.get())
    }

    @Test
    fun `IN_SWEEP is thread-local and independent per thread`() {
        SweepUtil.IN_SWEEP.set(true)
        var otherThreadValue = true

        val thread = Thread {
            otherThreadValue = SweepUtil.IN_SWEEP.get()
        }
        thread.start()
        thread.join()

        assertTrue(SweepUtil.IN_SWEEP.get(), "Current thread should be true")
        assertFalse(otherThreadValue, "Other thread should default to false")

        SweepUtil.IN_SWEEP.set(false)
    }

    @Test
    fun `IN_SWEEP try-finally pattern resets on completion`() {
        assertFalse(SweepUtil.IN_SWEEP.get())

        SweepUtil.IN_SWEEP.set(true)
        try {
            assertTrue(SweepUtil.IN_SWEEP.get())
        } finally {
            SweepUtil.IN_SWEEP.set(false)
        }

        assertFalse(SweepUtil.IN_SWEEP.get())
    }

    @Test
    fun `IN_SWEEP try-finally pattern resets on exception`() {
        assertFalse(SweepUtil.IN_SWEEP.get())

        try {
            SweepUtil.IN_SWEEP.set(true)
            throw RuntimeException("simulated crash")
        } catch (_: RuntimeException) {
        } finally {
            SweepUtil.IN_SWEEP.set(false)
        }

        assertFalse(SweepUtil.IN_SWEEP.get(), "Should reset even after exception")
    }
}
