package betterblockentities.mixin.render.immediate.blockentity;

/* local */
import betterblockentities.client.gui.config.ConfigCache;

/* minecraft */
import net.minecraft.client.renderer.blockentity.ChestRenderer;

/* mixin */
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ChestRenderer.class)
public class ChestRendererMixin {
    /** Applies Christmas texture override to immediate chest rendering. */
    @ModifyVariable(method = "getChestMaterial", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private static boolean bbe$useChristmasTextures(boolean vanillaChristmasTextures) {
        return vanillaChristmasTextures || (ConfigCache.optimizeChests && ConfigCache.christmasChests);
    }
}
