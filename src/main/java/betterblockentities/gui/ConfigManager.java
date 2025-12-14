package betterblockentities.gui;

/* local */
import betterblockentities.util.BlockEntityManager;

/* minecraft */
import net.minecraft.world.level.block.entity.*;

/* gson */
import org.spongepowered.include.com.google.gson.*;

/* java/misc */
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;

public class ConfigManager
{
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final File CONFIG_FILE =
            new File("config", "betterblockentities.json");

    public static ConfigHolder CONFIG = new ConfigHolder();

    public static void load() {
        if (CONFIG_FILE.exists()) {
            try (FileReader reader = new FileReader(CONFIG_FILE)) {
                CONFIG = GSON.fromJson(reader, ConfigHolder.class);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            save();
        }
    }

    public static void save() {
        CONFIG_FILE.getParentFile().mkdirs();
        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            GSON.toJson(CONFIG, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void refreshSupportedTypes() {
        /* set supported block entities in BlockEntityManager */
        ReferenceOpenHashSet<Class<? extends BlockEntity>> supported = new ReferenceOpenHashSet<>();
        if (ConfigManager.CONFIG.optimize_chests) {
            supported.add(ChestBlockEntity.class);
            supported.add(TrappedChestBlockEntity.class);
            supported.add(EnderChestBlockEntity.class);
        }
        if (ConfigManager.CONFIG.optimize_shulkers) supported.add(ShulkerBoxBlockEntity.class);
        if (ConfigManager.CONFIG.optimize_bells) supported.add(BellBlockEntity.class);
        if (ConfigManager.CONFIG.optimize_decoratedpots) supported.add(DecoratedPotBlockEntity.class);
        if (ConfigManager.CONFIG.optimize_beds) supported.add(BedBlockEntity.class);
        if (ConfigManager.CONFIG.optimize_signs) {
            supported.add(SignBlockEntity.class);
            supported.add(HangingSignBlockEntity.class);
        }
        if (ConfigManager.CONFIG.optimize_banners) supported.add(BannerBlockEntity.class);
        BlockEntityManager.SUPPORTED_TYPES = supported;

        /* set animation/rendering config values in BlockEntityManager */
        var cfg = ConfigManager.CONFIG;
        BlockEntityManager.chestAnims = cfg.chest_animations;
        BlockEntityManager.shulkerAnims = cfg.shulker_animations;
        BlockEntityManager.bellAnims = cfg.bell_animations;
        BlockEntityManager.potAnims = cfg.pot_animations;
        BlockEntityManager.signText = cfg.render_sign_text;
        BlockEntityManager.masterOptimize = cfg.master_optimize;
        BlockEntityManager.smoothness = cfg.smoothness_slider;
    }
}
