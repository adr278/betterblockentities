package betterblockentities.client.gui.storage;

/* local */
import betterblockentities.client.gui.config.BBEConfig;
import betterblockentities.platform.GlobalScope;

public class SodiumConfigStorage {
    public SodiumConfigStorage() { }
    public void save() {
        BBEConfig.save(GlobalScope.CONFIG.MAIN);
        BBEConfig.save(GlobalScope.CONFIG.EXPERIMENTAL);
        //BBEConfig.save(BBE.CONFIG.HIDDEN);
    }
}
