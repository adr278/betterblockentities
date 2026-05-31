package betterblockentities.mixin.sodium.config;

import betterblockentities.client.chunk.section.SectionUpdateDispatcher;
import betterblockentities.client.gui.config.BBEConfig;
import betterblockentities.client.gui.config.ConfigCache;
import net.caffeinemc.mods.sodium.client.config.structure.Config;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * I couldn't find a builder option for "running some code after apply is pressed" so this will have to do
 */
@Mixin(Config.class)
public class ConfigMixin {
    @Inject(method = "applyAllOptions", at = @At("RETURN"))
    public void applyAllOptions(CallbackInfo ci) {
        final boolean beforeMasterOptimize = ConfigCache.masterOptimize;
        final boolean beforeOptimizeChests = ConfigCache.optimizeChests;
        final boolean beforeOptimizeSigns = ConfigCache.optimizeSigns;
        final boolean beforeOptimizeDecoratedPots = ConfigCache.optimizeDecoratedPots;
        final boolean beforeOptimizeBeds = ConfigCache.optimizeBeds;
        final boolean beforeOptimizeShulker = ConfigCache.optimizeShulker;
        final boolean beforeOptimizeBanners = ConfigCache.optimizeBanners;
        final boolean beforeOptimizeBells = ConfigCache.optimizeBells;
        final boolean beforeOptimizeCampfire = ConfigCache.optimizeCampfire;
        final boolean beforeChristmasChests = ConfigCache.christmasChests;
        final int beforeBannerGraphics = ConfigCache.bannerGraphics;
        final int beforeBannerPose = ConfigCache.bannerPose;

        BBEConfig.updateConfigCache();

        final boolean terrainAffectingChange =
                beforeMasterOptimize != ConfigCache.masterOptimize
                        || beforeOptimizeChests != ConfigCache.optimizeChests
                        || beforeOptimizeSigns != ConfigCache.optimizeSigns
                        || beforeOptimizeDecoratedPots != ConfigCache.optimizeDecoratedPots
                        || beforeOptimizeBeds != ConfigCache.optimizeBeds
                        || beforeOptimizeShulker != ConfigCache.optimizeShulker
                        || beforeOptimizeBanners != ConfigCache.optimizeBanners
                        || beforeOptimizeBells != ConfigCache.optimizeBells
                        || beforeOptimizeCampfire != ConfigCache.optimizeCampfire
                        || beforeChristmasChests != ConfigCache.christmasChests
                        || beforeBannerGraphics != ConfigCache.bannerGraphics
                        || beforeBannerPose != ConfigCache.bannerPose;

        if (terrainAffectingChange) {
            SectionUpdateDispatcher.queueUpdateAllSections();
        }
    }
}
