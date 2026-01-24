package betterblockentities.client.gui.config;

/* local */
import betterblockentities.client.BBE;
import betterblockentities.client.gui.config.builder.ConfigBuilder;
import betterblockentities.client.gui.option.*;
import betterblockentities.client.gui.storage.ConfigStorageCollection;
import betterblockentities.client.gui.storage.ConfigStorageObject;
import betterblockentities.client.gui.storage.ConfigStorageIdentifiers;
import betterblockentities.client.render.immediate.blockentity.BlockEntityManager;

/* minecraft */
import net.minecraft.world.level.block.entity.*;

/* gson */
import com.google.gson.*;

/* java/misc */
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Locale;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;

public class BBEConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File CONFIG_FILE = new File("config", "BBEConfig.json");

    private static final ConfigStorageCollection configStorageCollection = new ConfigStorageCollection();

    public ConfigStorageObject MAIN;
    public ConfigStorageObject EXPERIMENTAL;
    public ConfigStorageObject HIDDEN;

    public BBEConfig() { buildConfigStorages(); }

    /**
     * builds all of our config storages, loads options from config file if present, if not, writes default settings to file
     * each option is built in {@link ConfigBuilder}
     */
    private void buildConfigStorages() {
        /* build "dummy" storages (default settings) */
        ConfigStorageObject main = ConfigBuilder.buildMainStorage();
        ConfigStorageObject experimental = ConfigBuilder.buildExperimentalStorage();
        ConfigStorageObject hidden = ConfigBuilder.buildHiddenStorage();

        /* if a config file exists, update each ConfigStorageObject -> { OptionObject, .... } with saved value from the config file */
        if (CONFIG_FILE.exists()) {
            load(main);
            load(experimental);
            load(hidden);
        }

        /* no config file found, create and write defaults to file */
        else {
            save(main);
            save(experimental);
        }

        /* append each storage to the storage collection */
        configStorageCollection.addStorage(ConfigStorageIdentifiers.MAIN, main);
        configStorageCollection.addStorage(ConfigStorageIdentifiers.EXPERIMENTAL, experimental);
        configStorageCollection.addStorage(ConfigStorageIdentifiers.HIDDEN, hidden);

        /* static "getters" for each storage */
        MAIN = configStorageCollection.getStorage(ConfigStorageIdentifiers.MAIN);
        EXPERIMENTAL = configStorageCollection.getStorage(ConfigStorageIdentifiers.EXPERIMENTAL);
        HIDDEN = configStorageCollection.getStorage(ConfigStorageIdentifiers.HIDDEN);
    }

    /**
     * load config storage from root object inside config file
     */
    public static void load(ConfigStorageObject storage) {
        String storageId = storage.getStorageId();

        if (storage == null) {
            throw new IllegalArgumentException("storage must not be null");
        }
        if (storageId == null || storageId.isBlank()) {
            throw new IllegalArgumentException("storageId must not be blank");
        }
        if (!CONFIG_FILE.exists()) return;

        try (FileReader reader = new FileReader(CONFIG_FILE)) {
            JsonElement parsed = JsonParser.parseReader(reader);
            if (parsed == null || !parsed.isJsonObject()) return;

            JsonObject root = parsed.getAsJsonObject();
            JsonElement arrEl = root.get(storageId);
            if (arrEl == null || !arrEl.isJsonArray()) return;

            JsonArray array = arrEl.getAsJsonArray();

            for (JsonElement el : array) {
                if (el == null || !el.isJsonObject()) continue;

                JsonObject entry = el.getAsJsonObject();

                JsonElement keyEl = entry.get("option");
                JsonElement valEl = entry.get("value");
                if (keyEl == null || keyEl.isJsonNull() || valEl == null) continue;

                String key = keyEl.getAsString().toLowerCase(Locale.ROOT);

                /* existing option in default storage */
                OptionObject<?> opt = storage.getOption(key);
                if (opt == null) continue; //unknown option in file, ignore

                if (valEl.isJsonNull()) {
                    continue; //skip (keep defaults)
                }

                /* apply value based on option type */
                if (valEl.isJsonPrimitive()) {
                    if (opt instanceof BooleanOption boolOpt) {
                        boolOpt.setValue(valEl.getAsBoolean());
                    } else if (opt instanceof IntegerOption intOpt) {
                        intOpt.setValue(valEl.getAsInt());
                    } else if (opt instanceof FloatOption floatOpt) {
                        floatOpt.setValue(valEl.getAsFloat());
                    } else {
                        opt.setValue(null); //this is fine as this option is of an invalid type
                    }
                }
            }

        } catch (Exception ex) {
            //failed to read : handle this as well
            ex.printStackTrace();
        }
    }

    /**
     * write config storage to root object inside config file
     */
    public static void save(ConfigStorageObject storage) {
        try {
            File parent = CONFIG_FILE.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                throw new IOException("Could not create config directory: " + parent);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        /* read existing file (or start fresh) */
        JsonObject root = new JsonObject();
        if (CONFIG_FILE.exists()) {
            try (FileReader reader = new FileReader(CONFIG_FILE)) {
                JsonElement parsed = JsonParser.parseReader(reader);
                if (parsed != null && parsed.isJsonObject()) {
                    root = parsed.getAsJsonObject();
                }
            } catch (Exception e) {
                //corrupted file, handle this by resetting to defaults?
                e.printStackTrace();
                root = new JsonObject();
            }
        }

        /* build JSON array for this storage */
        JsonArray array = new JsonArray();
        storage.getAllOptions().forEach((key, option) -> {
            JsonObject entry = new JsonObject();
            entry.addProperty("option", key);

            Object value = option.getValue();
            if (value instanceof Boolean b) entry.addProperty("value", b);
            else if (value instanceof Number n) entry.addProperty("value", n);
            else if (value instanceof String s) entry.addProperty("value", s);
            else if (value == null) entry.add("value", JsonNull.INSTANCE);
            else entry.addProperty("value", String.valueOf(value));

            array.add(entry);
        });

        /* put it into the root (this updates this storage only) */
        String storageId = storage.getStorageId();
        root.add(storageId, array);

        /* write the full root back (preserves other storages) */
        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            GSON.toJson(root, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * cache the main config options to avoid repeated lookups in the config storage
     */
    public static void updateConfigCache() {
        ReferenceOpenHashSet<Class<? extends BlockEntity>> supported = new ReferenceOpenHashSet<>();

        ConfigCache.masterOptimize = BBE.OPTIONS.master();
        ConfigCache.chestAnims = BBE.OPTIONS.animateChest();
        ConfigCache.shulkerAnims = BBE.OPTIONS.animateShulker();
        ConfigCache.bellAnims = BBE.OPTIONS.animateBell();
        ConfigCache.potAnims = BBE.OPTIONS.animateDecoratedpot();
        ConfigCache.renderpasses = BBE.OPTIONS.extraRenderPasses();
        ConfigCache.signText = BBE.OPTIONS.signText();
        ConfigCache.signTextRenderDistance = BBE.OPTIONS.signTextDistance();
        ConfigCache.optimizeSigns = BBE.OPTIONS.optimizeSign();
        ConfigCache.christmasChests = BBE.OPTIONS.useChristmasChestTextures();
        ConfigCache.optimizeChests = BBE.OPTIONS.optimizeChests();
        ConfigCache.optimizeDecoratedPots = BBE.OPTIONS.optimizeDecoratedPot();
        ConfigCache.optimizeBeds = BBE.OPTIONS.optimizeBed();
        ConfigCache.optimizeShulker = BBE.OPTIONS.optimizeShulker();
        ConfigCache.bannerGraphics = BBE.OPTIONS.bannerGraphics();
        ConfigCache.optimizeBells = BBE.OPTIONS.optimizeBell();
        ConfigCache.optimizeBanners = BBE.OPTIONS.optimizeBanner();
        ConfigCache.optimizeCopperGolemStatue = BBE.OPTIONS.optimizeCopperGolemStatue();
        ConfigCache.updateType = BBE.LoadedModList.EMF ? EnumTypes.UpdateSchedulerType.SMART.ordinal() : BBE.OPTIONS.updateScheduler();
        ConfigCache.signTextCulling = BBE.OPTIONS.signTextCulling();

        if (ConfigCache.optimizeChests) {
            supported.add(ChestBlockEntity.class);
            supported.add(TrappedChestBlockEntity.class);
            supported.add(EnderChestBlockEntity.class);
        }
        if (ConfigCache.optimizeShulker) supported.add(ShulkerBoxBlockEntity.class);
        if (ConfigCache.optimizeSigns) {
            supported.add(SignBlockEntity.class);
            supported.add(HangingSignBlockEntity.class);
        }
        if (ConfigCache.optimizeDecoratedPots) supported.add(DecoratedPotBlockEntity.class);
        if (ConfigCache.optimizeBanners) supported.add(BannerBlockEntity.class);
        if (ConfigCache.optimizeBells) supported.add(BellBlockEntity.class);
        if (ConfigCache.optimizeBeds) supported.add(BedBlockEntity.class);
        if (ConfigCache.optimizeCopperGolemStatue) supported.add(CopperGolemStatueBlockEntity.class);

        BlockEntityManager.SUPPORTED_TYPES = supported;
    }
}
