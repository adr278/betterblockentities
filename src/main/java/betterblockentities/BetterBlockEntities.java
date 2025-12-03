package betterblockentities;

/* local */
import betterblockentities.gui.ConfigManager;

/* fabric */
import betterblockentities.util.BlockEntityTracker;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
    TODO: Add support for vanilla fabric (so the mod can run without
     sodium too)
    TODO: Improve signs, make them completely baked into meshes (sign text)
    TODO: Add support for pottery patterns (decorated pots)
    TODO: Check sign ticker logic and implement same approach from the other BE´s
          if possible
    TODO: Other Block Entities...
*/

public class BetterBlockEntities implements ClientModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger("BBE-Logger");
    private static boolean lastScreen = false;

    public static Frustum curFrustum;

    @Override
    public void onInitializeClient() {
        /* register our model loader */
        BetterBlockEntities.getLogger().info("Registering Model Loading Plugin");
        ModelLoadingPlugin.register(pluginContext -> { new ModelLoader().initialize(pluginContext); });

        /* load config from disk file */
        BetterBlockEntities.getLogger().info("Loading saved Config");
        ConfigManager.load();

        /* updates the list of supported block entity types and cached config in BlockEntityManager */
        BetterBlockEntities.getLogger().info("Updating supported block entities");
        ConfigManager.refreshSupportedTypes();

        /* validate animMap */
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.level == null) return;
            BlockEntityTracker.animMap.removeIf(pos -> client.level.getBlockEntity(BlockPos.of(pos)) == null);
        });
    }

    public static Logger getLogger() {
        return LOGGER;
    }
 }
