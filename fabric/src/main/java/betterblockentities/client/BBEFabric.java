package betterblockentities.client;

/* local */
import betterblockentities.platform.FabricPlatformAccess;
import betterblockentities.platform.GlobalScope;
import betterblockentities.client.gui.config.BBEConfig;

/* fabric */
import net.fabricmc.api.ClientModInitializer;

public class BBEFabric implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        GlobalScope.platformAccess = new FabricPlatformAccess();

        GlobalScope.LOGGER.info("Checking for loaded mods for compact...");
        GlobalScope.ModCompact.checkForLoadedMods();

        GlobalScope.LOGGER.info("Building config cache from user defined config file...");
        BBEConfig.updateConfigCache();

        GlobalScope.LOGGER.info("Collecting API Entrypoint's...");
        GlobalScope.ApiInternal.collectEntryPoints();
    }
 }
