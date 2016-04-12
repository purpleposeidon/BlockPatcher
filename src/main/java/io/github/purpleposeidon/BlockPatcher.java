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
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.StringUtils;
import net.minecraft.world.MinecraftException;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.NibbleArray;
import net.minecraft.world.chunk.storage.AnvilChunkLoader;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import net.minecraft.world.chunk.storage.IChunkLoader;
import net.minecraft.world.chunk.storage.RegionFile;
import net.minecraft.world.storage.ISaveHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.ConfigCategory;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.event.world.WorldEvent;
import org.apache.logging.log4j.Logger;

import java.io.DataInputStream;
import java.io.DataOutputStream;
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
    int[] transforms = new int[MAX_BLOCK_ID];

    @Mod.EventHandler
    public void preinit(FMLPreInitializationEvent event) {
        for (int i = 0; i < transforms.length; i++) transforms[i] = i;

        MinecraftForge.EVENT_BUS.register(this);
        FMLCommonHandler.instance().bus().register(this);
        log = event.getModLog();
        configFile = event.getSuggestedConfigurationFile();
        config = new Configuration(configFile);
        block_patches = config.getCategory("blockPatches");
        item_patches = config.getCategory("itemPatches");
        config.save();
        for (Map.Entry<String, Property> entry : block_patches.entrySet()) {
            String idS = entry.getKey();
            String repl = entry.getValue().getString();
            int id = Integer.parseInt(idS);
            transforms[id] = parsePatch(id, repl);
        }
    }

    private int parsePatch(int defaultId, String repl) {
        if (StringUtils.isNullOrEmpty(repl)) return defaultId;
        return GameData.getBlockRegistry().getId(repl);
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
    void log(Object... msg) {
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
        final FMLControlledNamespacedRegistry<Block> blockRegistry = GameData.getBlockRegistry();
        int regionFiles = 0;
        for (File regionName : files) {
            if (!regionName.toString().endsWith(".mca")) continue;
            regionFiles++;
        }
        int regionFileIndex = 0;
        for (File regionName : files) {
            if (!regionName.toString().endsWith(".mca")) continue;
            regionFileIndex++;
            RegionFile rf = new RegionFile(regionName);
            int changes = 0;
            int chunkCount = 0;
            for (int rx = 0; rx < 32; rx++) {
                for (int rz = 0; rz < 32; rz++) {
                    // A region is a sparse 32x32 of chunks. See RegionFile.outOfBounds()
                    if (!rf.chunkExists(rx, rz)) continue;
                    chunkCount++;
                    DataInputStream dis = rf.getChunkDataInputStream(rx, rz);
                    NBTTagCompound tag = CompressedStreamTools.read(dis);
                    dis.close();
                    if (tag == null) continue;
                    NBTTagCompound Level = tag.getCompoundTag("Level");
                    if (Level == null) continue;
                    int chunkX = Level.getInteger("xPos");
                    int chunkZ = Level.getInteger("zPos");
                    NBTTagList Sections = Level.getTagList("Sections", Constants.NBT.TAG_COMPOUND);
                    if (Sections == null) continue;
                    boolean changed = false;
                    // We simply have to operate on raw NBT data. There's far too much modded/vanilla squirreliness
                    // going on to load the chunk through normal mechanisms.
                    // See AnvilChunkLoader.readChunkFromNBT and ExtendedBlockStorage
                    for (int sId = 0; sId < Sections.tagCount(); sId++) {
                        NBTTagCompound Section = Sections.getCompoundTagAt(sId);
                        byte Y = Section.getByte("Y");
                        ExtendedBlockStorage ebs = new ExtendedBlockStorage(Y << 4, false /* hasSky. We don't need it. */);
                        byte[] LSB = Section.getByteArray("Blocks");
                        ebs.setBlockLSBArray(LSB);
                        NibbleArray MSBA = null;

                        if (Section.hasKey("Add", Constants.NBT.TAG_BYTE_ARRAY)) {
                            ebs.setBlockMSBArray(MSBA = new NibbleArray(Section.getByteArray("Add"), 4));
                        }

                        ebs.setBlockMetadataArray(new NibbleArray(Section.getByteArray("Data"), 4));

                        for (int slabY = 0; slabY < 0x10; slabY++) {
                            for (int slabZ = 0; slabZ < 0x10; slabZ++) {
                                for (int slabX = 0; slabX < 0x10; slabX++) {
                                    // This iteration order has us stepping through the LSB efficiently.
                                    int i = slabY << 8 | slabZ << 4 | slabX;
                                    int oldId = LSB[i] & 255;
                                    if (MSBA != null) {
                                        oldId |= MSBA.get(slabX, slabY, slabZ) << 8;
                                    }
                                    int replacement = transforms[oldId];
                                    if (replacement == oldId) continue;
                                    ebs.func_150818_a/* setBlock */(slabX, slabY, slabZ, blockRegistry.getObjectById(replacement));
                                    MSBA = ebs.getBlockMSBArray();
                                    changed = true;
                                }
                            }
                        }

                        if (changed && MSBA != null && !Section.hasKey("Add")) {
                            Section.setByteArray("Add", MSBA.data);
                        }
                    }

                    if (!changed) continue;
                    changes++;
                    // NOTE: Relies on the data arrays being impure
                    DataOutputStream dos = rf.getChunkDataOutputStream(rx, rz);
                    CompressedStreamTools.write(tag, dos);
                    dos.close();
                }
            }
            String perc = "[" + (regionFileIndex * 100 / regionFiles) + "%]";
            log(perc + regionName.toString() + ": " + changes + " / " + chunkCount + " chunks modified");
        }
    }

}
