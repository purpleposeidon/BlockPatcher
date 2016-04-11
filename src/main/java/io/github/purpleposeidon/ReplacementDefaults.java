package io.github.purpleposeidon;

import net.minecraft.block.Block;
import net.minecraft.block.BlockOre;
import net.minecraft.block.BlockRailBase;
import net.minecraft.block.material.Material;
import net.minecraft.init.Blocks;

import java.lang.reflect.Method;

public class ReplacementDefaults {
    static Block reflectionGet(Block b) {
        Object ret = null;
        try {
            Method m = b.getClass().getMethod("blockPatcher$getReplacement");
            ret = m.invoke(b);
        } catch (Throwable t) {
            // Yum!
        }
        return (Block) ret;
    }

    static Block getSuggestedBlock(Block b) {
        Material mat = b.getMaterial();
        Block vanilla = reflectionGet(b);
        if (vanilla != null) return vanilla;
        if (mat == Material.air) {
            vanilla = Blocks.air;
        } else if (mat == Material.grass) {
            vanilla = Blocks.grass;
        } else if (mat == Material.ground) {
            vanilla = Blocks.dirt;
        } else if (mat == Material.wood) {
            vanilla = Blocks.log;
        } else if (mat == Material.rock) {
            vanilla = Blocks.stone;
        } else if (mat == Material.iron) {
            vanilla = Blocks.stone;
        } else if (mat == Material.anvil) {
            vanilla = Blocks.cobblestone;
        } else if (mat == Material.water) {
            vanilla = Blocks.water;
        } else if (mat == Material.lava) {
            vanilla = Blocks.lava;
        } else if (mat == Material.leaves) {
            vanilla = Blocks.leaves;
        } else if (mat == Material.plants) {
            vanilla = Blocks.air;
        } else if (mat == Material.vine) {
            vanilla = Blocks.air;
        } else if (mat == Material.sponge) {
            vanilla = Blocks.cobblestone;
        } else if (mat == Material.cloth) {
            vanilla = Blocks.wool;
        } else if (mat == Material.fire) {
            vanilla = Blocks.air;
        } else if (mat == Material.sand) {
            vanilla = Blocks.sand;
        } else if (mat == Material.circuits) {
            vanilla = Blocks.air;
        } else if (mat == Material.carpet) {
            vanilla = Blocks.carpet;
        } else if (mat == Material.glass) {
            vanilla = Blocks.stained_glass;
        } else if (mat == Material.redstoneLight) {
            vanilla = Blocks.air;
        } else if (mat == Material.tnt) {
            vanilla = Blocks.cobblestone;
        } else if (mat == Material.coral) {
            vanilla = Blocks.mossy_cobblestone;
        } else if (mat == Material.ice) {
            return Blocks.ice;
        } else if (mat == Material.packedIce) {
            return Blocks.packed_ice;
        } else if (mat == Material.snow) {
            vanilla = Blocks.snow;
        } else if (mat == Material.craftedSnow) {
            vanilla = Blocks.snow_layer;
        } else if (mat == Material.cactus) {
            vanilla = Blocks.air;
        } else if (mat == Material.clay) {
            vanilla = Blocks.stone;
        } else if (mat == Material.gourd) {
            vanilla = Blocks.air;
        } else if (mat == Material.dragonEgg) {
            vanilla = Blocks.air;
        } else if (mat == Material.portal) {
            return Blocks.air;
        } else if (mat == Material.cake) {
            vanilla = Blocks.air;
        } else if (mat == Material.web) {
            vanilla = Blocks.web;
        } else {
            if (b instanceof BlockOre) {
                vanilla = Blocks.stone;
            } else if (b.isOpaqueCube()) {
                vanilla = Blocks.cobblestone;
            } else {
                vanilla = Blocks.air;
            }
        }
        if (!b.isOpaqueCube()) {
            vanilla = Blocks.air;
        }
        if (b instanceof BlockRailBase) {
            b = Blocks.rail;
        }
        try {
            if (b.getBlockHardness(null, 0, 0, 0) < 0) {
                vanilla = Blocks.bedrock;
            }
        } catch (Throwable t) {
            // swallow
        }
        return vanilla;
    }
}
