package betterblockentities.client.gui.storage;

/* local */
import betterblockentities.client.BBE;
import betterblockentities.client.gui.config.BBEConfig;

public class SodiumConfigStorage {
    public SodiumConfigStorage() { }
    public void save() {
        BBEConfig.save(BBE.CONFIG.MAIN);
        BBEConfig.save(BBE.CONFIG.EXPERIMENTAL);
        //BBEConfig.save(BBE.CONFIG.HIDDEN);
    }
}
