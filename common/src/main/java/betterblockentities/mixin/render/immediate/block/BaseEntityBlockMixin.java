package betterblockentities.mixin.render.immediate.block;

/* local */
import betterblockentities.client.gui.config.ConfigCache;
import betterblockentities.render.AltRenderers;

/* minecraft */
import net.minecraft.world.level.block.BannerBlock;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CeilingHangingSignBlock;
import net.minecraft.world.level.block.DecoratedPotBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.WallBannerBlock;
import net.minecraft.world.level.block.WallHangingSignBlock;
import net.minecraft.world.level.block.WallSignBlock;
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
    private void forceModelRenderShape(final BlockState state, final CallbackInfoReturnable<RenderShape> cir) {
        if (!ConfigCache.masterOptimize) {
            return;
        }

        final Block block = state.getBlock();

        if (block instanceof BannerBlock || block instanceof WallBannerBlock) {
            if (ConfigCache.optimizeBanners && !AltRenderers.hasRendererOverride(BlockEntityType.BANNER)) {
                cir.setReturnValue(RenderShape.MODEL);
            }
            return;
        }

        if (block instanceof StandingSignBlock || block instanceof WallSignBlock) {
            if (ConfigCache.optimizeSigns && !AltRenderers.hasRendererOverride(BlockEntityType.SIGN)) {
                cir.setReturnValue(RenderShape.MODEL);
            }
            return;
        }

        if (block instanceof CeilingHangingSignBlock || block instanceof WallHangingSignBlock) {
            if (ConfigCache.optimizeSigns && !AltRenderers.hasRendererOverride(BlockEntityType.HANGING_SIGN)) {
                cir.setReturnValue(RenderShape.MODEL);
            }
            return;
        }

        if (block instanceof DecoratedPotBlock) {
            if (ConfigCache.optimizeDecoratedPots && !AltRenderers.hasRendererOverride(BlockEntityType.DECORATED_POT)) {
                cir.setReturnValue(RenderShape.MODEL);
            }
        }
    }
}
