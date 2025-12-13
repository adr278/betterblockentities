package betterblockentities;

/* local */
import betterblockentities.gui.ConfigManager;

/* fabric */
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.renderer.culling.Frustum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BetterBlockEntities implements ClientModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger("BBE-Logger");
    public static Frustum curFrustum;

    @Override
    public void onInitializeClient() {
        /* load config from disk file */
        BetterBlockEntities.getLogger().info("Loading saved Config");
        ConfigManager.load();

        /* updates the list of supported block entity types and cached config in BlockEntityManager */
        BetterBlockEntities.getLogger().info("Updating supported block entities");
        ConfigManager.refreshSupportedTypes();
    }

    public static Logger getLogger() {
        return LOGGER;
    }
 }
