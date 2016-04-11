package io.github.purpleposeidon;


import com.google.common.base.Joiner;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLMissingMappingsEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.registry.FMLControlledNamespacedRegistry;
import cpw.mods.fml.common.registry.GameData;
import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraft.block.Block;
import net.minecraft.block.BlockOre;
import net.minecraft.block.BlockRailBase;
import net.minecraft.block.material.Material;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraftforge.common.config.ConfigCategory;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.lang.reflect.Method;

@Mod(modid = "blockpatcher", name = "BlockPatcher")
public class BlockPatcher {
    File configFile;
    ConfigCategory block_patches;
    ConfigCategory item_patches;
    Configuration config;
    Logger log;

    @Mod.EventHandler
    public void preinit(FMLPreInitializationEvent event) {
        log = event.getModLog();
        configFile = event.getSuggestedConfigurationFile();
        config = new Configuration(configFile);
        block_patches = config.getCategory("blockPatches");
        block_patches.setComment("Format example:\n\tS:\"minecraft:stone\"=\"minecraft:bedrock\"");
        item_patches = config.getCategory("itemPatches");
        config.save();
    }

    @Mod.EventHandler
    public void saveSuggestions(FMLPostInitializationEvent event) {
        ConfigCategory sug = config.getCategory("suggested");
        FMLControlledNamespacedRegistry<Block> blockReg = GameData.getBlockRegistry();
        int defined = 0;
        for (Object obj : blockReg) defined++;
        if (defined == sug.size()) {
            log("Probably don't need to update the suggestions list. Delete that section of the config file to force an update.");
            return;
        }
        log("Making a list of recommendations");
        for (Object x : blockReg) {
            Block b = (Block) x;
            Block vanilla;
            vanilla = getSuggestedBlock(b);
            String name = blockReg.getNameForObject(b);
            String replName = blockReg.getNameForObject(vanilla);
            sug.put(name, new Property(name, replName, Property.Type.STRING));
        }
        config.save();
    }

    Joiner j = Joiner.on(" ");
    void log(String... msg) {
        log.warn(j.join(msg));
    }

    @Mod.EventHandler
    public void replace(FMLMissingMappingsEvent event) {
        for (FMLMissingMappingsEvent.MissingMapping missed : event.getAll()) {
            ConfigCategory src = missed.type == GameRegistry.Type.BLOCK ? block_patches : item_patches;
            Property got = src.get(missed.name);
            if (got == null) {
                log("No config entry provided for replacing", missed.name);
                continue;
            }
            String replName = got.getString();
            if (missed.type == GameRegistry.Type.BLOCK) {
                Block repl = GameData.getBlockRegistry().getObject(replName);
                if (repl == null || repl == Blocks.air) {
                    log("Skipping defined replacement for", missed.name, "as it is boring (evaluates to:", "" + repl, ")");
                    continue;
                }
                missed.remap(repl);
            } else if (missed.type == GameRegistry.Type.ITEM) {
                Item repl = GameData.getItemRegistry().getObject(replName);
                if (repl == null) {
                    log("Skipping defined replacement for", missed.name, "as it is boring (evaluates to:", "" + repl, ")");
                    continue;
                }
                missed.remap(repl);
            }
        }
    }

    private Block reflectionGet(Block b) {
        Object ret = null;
        try {
            Method m = b.getClass().getMethod("blockPatcher$getReplacement");
            ret = m.invoke(b);
        } catch (Throwable t) {
            // Yum!
        }
        return (Block) ret;
    }

    private Block getSuggestedBlock(Block b) {
        Material mat = b.getMaterial();
        Block vanilla;
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
