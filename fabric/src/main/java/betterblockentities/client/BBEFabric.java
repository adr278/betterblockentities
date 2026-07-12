package betterblockentities.client;

import betterblockentities.client.gui.DebugScreen;
import betterblockentities.client.gui.config.BBEConfig;
import betterblockentities.mixin.accessors.DebugScreenEntriesAccessor;
import betterblockentities.platform.FabricPlatformAccess;
import betterblockentities.platform.GlobalScope;
import net.fabricmc.api.ClientModInitializer;

public class BBEFabric implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        GlobalScope.platformAccess = new FabricPlatformAccess();

        DebugScreenEntriesAccessor.bbe$register(GlobalScope.DEBUG_ID, new DebugScreen());

        GlobalScope.LOGGER.info("Building config cache from user defined config file...");
        BBEConfig.updateConfigCache();

        GlobalScope.LOGGER.info("Collecting API Entrypoint's...");
        GlobalScope.ApiInternal.collectEntryPoints();
    }
}
