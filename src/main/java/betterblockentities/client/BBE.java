package betterblockentities.client;

/* local */
import betterblockentities.client.gui.config.BBEConfig;
import betterblockentities.client.gui.config.wrapper.GenericConfigWrapper;

/* fabric */
import net.fabricmc.api.ClientModInitializer;

/* minecraft */
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.renderer.culling.Frustum;

/* java/misc */
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BBE implements ClientModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger("BBE-Logger");

    /* BBEConfig contains generic options, GenericConfigWrapper returns correctly cast data types which are easier to work with */
    public static final BBEConfig CONFIG = new BBEConfig();
    public static final GenericConfigWrapper OPTIONS = new GenericConfigWrapper();

    public static Frustum curFrustum;

    @Override
    public void onInitializeClient() {
        LOGGER.info("BBE Loaded. Setting up assets!");
        LoadedModList.checkForLoadedMods();
        BBEConfig.updateConfigCache();
    }

    /* global logger, used for info logging, error handling, etc... */
    public static Logger getLogger() {
        return LOGGER;
    }

    public static class LoadedModList {
        public static boolean EMF = false;
        public static boolean NVIDIUM = false;

        public static void checkForLoadedMods() {
            if (FabricLoader.getInstance().isModLoaded("entity_model_features") &&
                    !((boolean)CONFIG.HIDDEN.getOption("override.forced_updatescheduler").getValue())) {
                EMF = true;
            }
            if (FabricLoader.getInstance().isModLoaded("nvidium")) {
                NVIDIUM = true;
            }
        }
    }
 }
