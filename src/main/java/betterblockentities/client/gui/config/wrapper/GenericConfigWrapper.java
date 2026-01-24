package betterblockentities.client.gui.config.wrapper;

/* local */
import betterblockentities.client.BBE;

/**
 * getters for all of our generic options, casts each option to the correct data type
 */
public class GenericConfigWrapper {
    public GenericConfigWrapper() { }
    
    private boolean getGenericBooleanOption(String key) {
        return (boolean)BBE.CONFIG.MAIN.getOption(key).getValue();
    }

    private int getGenericIntegerOption(String key) {
        return (int)BBE.CONFIG.MAIN.getOption(key).getValue();
    }

    public boolean master() {
        return getGenericBooleanOption("optimize.master");
    }

    public boolean optimizeChests() {
        return getGenericBooleanOption("optimize.chest");
    }

    public boolean optimizeShulker() {
        return getGenericBooleanOption("optimize.shulker");
    }

    public boolean optimizeSign() {
        return getGenericBooleanOption("optimize.sign");
    }

    public boolean optimizeDecoratedPot() {
        return getGenericBooleanOption("optimize.decoratedpot");
    }

    public boolean optimizeBanner() {
        return getGenericBooleanOption("optimize.banner");
    }

    public boolean optimizeBell() { return getGenericBooleanOption("optimize.bell"); }

    public boolean optimizeBed() {
        return getGenericBooleanOption("optimize.bed");
    }

    public boolean optimizeCopperGolemStatue() {
        return getGenericBooleanOption("optimize.copper_golem_statue");
    }

    public boolean animateChest() {
        return getGenericBooleanOption("animation.chest");
    }

    public boolean animateShulker() {
        return getGenericBooleanOption("animation.shulker");
    }

    public boolean animateBell() {
        return getGenericBooleanOption("animation.bell");
    }

    public boolean animateDecoratedpot() {
        return getGenericBooleanOption("animation.decoratedpot");
    }

    public int bannerGraphics() {
        return getGenericIntegerOption("misc.banner_graphics");
    }

    public boolean useChristmasChestTextures() {
        return getGenericBooleanOption("misc.christmas_chest");
    }

    public int extraRenderPasses() { return getGenericIntegerOption("misc.render_passes"); }

    public int signTextDistance() {
        return getGenericIntegerOption("misc.sign_text_distance");
    }

    public boolean signText() {
        return getGenericBooleanOption("misc.sign_text");
    }

    public boolean signTextCulling() {
        return getGenericBooleanOption("misc.sign_text_culling");
    }

    public int updateScheduler() {
        return getGenericIntegerOption("misc.update_scheduler");
    }
}
