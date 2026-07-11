package betterblockentities.client;

/* local */
import betterblockentities.client.gui.config.BBEConfig;
import betterblockentities.platform.GlobalScope;
import betterblockentities.platform.NeoForgePlatformAccess;

/* neoforge */
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;

@Mod(value = GlobalScope.MOD_ID, dist = Dist.CLIENT)
public class BBENeoForge {
    public BBENeoForge() {
        GlobalScope.platformAccess = new NeoForgePlatformAccess();

        GlobalScope.LOGGER.info("Checking for loaded mods for compact...");

        GlobalScope.LOGGER.info("Building config cache from user defined config file...");
        BBEConfig.updateConfigCache();

        GlobalScope.LOGGER.info("Collecting API Entrypoint's...");
        GlobalScope.ApiInternal.collectEntryPoints();
    }
}
