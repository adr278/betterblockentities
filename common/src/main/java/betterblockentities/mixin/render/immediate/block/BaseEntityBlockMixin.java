package betterblockentities.mixin.render.immediate.block;

/* local */
import betterblockentities.client.gui.config.ConfigCache;
import betterblockentities.client.render.immediate.util.VanillaBlockSupport;
import betterblockentities.render.AltRenderers;

/* minecraft */
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/* mixin */
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BaseEntityBlock.class)
public abstract class BaseEntityBlockMixin {
    @Inject(method = "getRenderShape", at = @At("HEAD"), cancellable = true)
    @SuppressWarnings("unused")
    private void forceModelRenderShape(final BlockState blockState, final CallbackInfoReturnable<RenderShape> cir) {
        if (!ConfigCache.masterOptimize) {
            return;
        }

        if (VanillaBlockSupport.isVanillaBannerBlock(blockState)) {
            if (ConfigCache.optimizeBanners && !AltRenderers.hasRendererOverride(BlockEntityType.BANNER)) {
                cir.setReturnValue(RenderShape.MODEL);
            }
            return;
        }

        if (VanillaBlockSupport.isVanillaSignBlock(blockState)) {
            if (ConfigCache.optimizeSigns && !AltRenderers.hasRendererOverride(BlockEntityType.SIGN)) {
                cir.setReturnValue(RenderShape.MODEL);
            }
            return;
        }

        if (VanillaBlockSupport.isVanillaHangingSignBlock(blockState)) {
            if (ConfigCache.optimizeSigns && !AltRenderers.hasRendererOverride(BlockEntityType.HANGING_SIGN)) {
                cir.setReturnValue(RenderShape.MODEL);
            }
            return;
        }

        if (VanillaBlockSupport.isVanillaDecoratedPotBlock(blockState)) {
            if (ConfigCache.optimizeDecoratedPots && !AltRenderers.hasRendererOverride(BlockEntityType.DECORATED_POT)) {
                cir.setReturnValue(RenderShape.MODEL);
            }
        }
    }
}
