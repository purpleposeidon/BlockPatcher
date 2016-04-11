package io.github.purpleposeidon;


import com.google.common.base.Joiner;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLMissingMappingsEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.registry.ExistingSubstitutionException;
import cpw.mods.fml.common.registry.FMLControlledNamespacedRegistry;
import cpw.mods.fml.common.registry.GameData;
import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraft.block.Block;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.StringUtils;
import net.minecraft.world.MinecraftException;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.AnvilChunkLoader;
import net.minecraft.world.chunk.storage.IChunkLoader;
import net.minecraft.world.chunk.storage.RegionFile;
import net.minecraft.world.storage.ISaveHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.ConfigCategory;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.event.world.WorldEvent;
import org.apache.logging.log4j.Logger;

import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.util.Map;

@Mod(modid = "blockpatcher", name = "BlockPatcher")
public class BlockPatcher {
    File configFile;
    ConfigCategory block_patches;
    ConfigCategory item_patches;
    Configuration config;
    Logger log;

    static final int MAX_BLOCK_ID = 4095;
    IPatch[] transforms = new IPatch[MAX_BLOCK_ID];

    @Mod.EventHandler
    public void preinit(FMLPreInitializationEvent event) {
        for (int i = 0; i < transforms.length; i++) transforms[i] = IPatch.NULL;

        MinecraftForge.EVENT_BUS.register(this);
        FMLCommonHandler.instance().bus().register(this);
        log = event.getModLog();
        configFile = event.getSuggestedConfigurationFile();
        config = new Configuration(configFile);
        block_patches = config.getCategory("blockPatches");
        item_patches = config.getCategory("itemPatches");
        block_patches.setComment("Format:\nI:id=minecraft:blockname#optionalMetadata");
        config.save();
        for (Map.Entry<String, Property> entry : block_patches.entrySet()) {
            String idS = entry.getKey();
            String repl = entry.getValue().getString();
            int id = Integer.parseInt(idS);
            transforms[id] = parsePatch(repl);
        }
    }

    private IPatch parsePatch(String repl) {
        if (StringUtils.isNullOrEmpty(repl)) return IPatch.NULL;
        String parts[] = repl.split("#");
        String id = parts[0];
        int md = -1;
        if (parts.length > 1) {
            md = Integer.parseInt(parts[1]);
        }
        Block b = GameData.getBlockRegistry().getObject(id);
        return new ReplaceWith(b, md);
    }

    static class ReplaceWith implements IPatch {
        final Block to;
        final int md;

        public ReplaceWith(Block to, int md) {
            this.to = to;
            this.md = md;
        }

        public ReplaceWith(Block to) {
            this.to = to;
            this.md = -1;
        }

        @Override
        public void apply(Chunk chunk, int cx, int y, int cz) {
            int md = this.md;
            if (md == -1) {
                md = chunk.getBlockMetadata(cx, y, cz);
            }
            chunk.func_150807_a(cx, y, cz, to, md);
        }
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
        log("Making a list of recommended block patches");
        for (Object x : blockReg) {
            Block b = (Block) x;
            Block vanilla;
            vanilla = ReplacementDefaults.getSuggestedBlock(b);
            int id = blockReg.getId(b);
            String name = blockReg.getNameForObject(b);
            String replName = blockReg.getNameForObject(vanilla);
            Property prop = new Property(Integer.toString(id), replName, Property.Type.STRING);
            prop.comment = name;
            sug.put(name, prop);
        }
        config.save();
    }

    Joiner j = Joiner.on(" ");
    void log(String... msg) {
        log.warn(j.join(msg));
    }

    @Mod.EventHandler
    public void replace(FMLMissingMappingsEvent event) throws ExistingSubstitutionException {
        for (FMLMissingMappingsEvent.MissingMapping missed : event.getAll()) {
            ConfigCategory src = missed.type == GameRegistry.Type.BLOCK ? block_patches : item_patches;
            Property got = src.get(missed.name);
            if (got == null) {
                log("No config entry provided for replacing", missed.name);
                continue;
            }
            log("Will be replaced: ", missed.name);
            missed.ignore();
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void patchWorld(WorldEvent.Load event) throws IOException, MinecraftException {
        if (event.world.isRemote) return;
        doPatch(event);
    }

    private void doPatch(WorldEvent.Load event) throws IOException, MinecraftException {
        ISaveHandler saveHandler = event.world.getSaveHandler();
        IChunkLoader loader = saveHandler.getChunkLoader(event.world.provider);
        File f = ((AnvilChunkLoader) loader).chunkSaveLocation;
        f = new File(f, "region");
        File[] files = f.listFiles();
        if (files == null) return;
        for (File regionName : files) {
            if (!regionName.toString().endsWith(".mca")) continue;
            log("Converting ", regionName.toString());
            RegionFile rf = new RegionFile(regionName);
            for (int rx = 0; rx < 32; rx++) {
                for (int rz = 0; rz < 32; rz++) {
                    if (!rf.chunkExists(rx, rz)) continue;
                    log("    Processing regionchunk: " + rx + "," + rz);
                    DataInputStream dis = rf.getChunkDataInputStream(rx, rz);
                    NBTTagCompound tag = CompressedStreamTools.read(dis);
                    dis.close();
                    if (tag == null) continue;
                    NBTTagCompound Level = tag.getCompoundTag("Level");
                    if (Level == null) continue;
                    int x = Level.getInteger("xPos");
                    int z = Level.getInteger("zPos");
                    Chunk chunk = loader.loadChunk(event.world, x, z);
                    if (chunk == null) continue;
                    MinecraftForge.EVENT_BUS.post(new ChunkEvent.Load(chunk));
                    processChunk(chunk);
                    if (!chunk.isModified) continue;
                    loader.saveChunk(event.world, chunk);
                    loader.saveExtraChunkData(event.world, chunk);
                }
            }

        }
    }

    private void processChunk(Chunk chunk) {
        for (int cx = 0; cx < 0x10; cx++) {
            for (int cz = 0; cz < 0x10; cz++) {
                for (int y = 0; y < 0x100; y++) {
                    processLocation(chunk, cx, y, cz);
                }
            }
        }
    }

    private void processLocation(Chunk chunk, int cx, int y, int cz) {
        Block b = chunk.getBlock(cx, y, cz);
        int id = GameData.getBlockRegistry().getId(b);
        transforms[id].apply(chunk, cx, y, cz);
    }

}
