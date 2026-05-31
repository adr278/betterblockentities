package betterblockentities.mixin.render.immediate.block;

/* local */
import betterblockentities.client.gui.config.ConfigCache;
import betterblockentities.render.AltRenderers;

/* minecraft */
import net.minecraft.world.level.block.EnderChestBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/* mixin */
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EnderChestBlock.class)
public abstract class EnderChestBlockMixin {
    @Inject(method = "getRenderShape", at = @At("HEAD"), cancellable = true)
    private void forceModelRenderShape(final BlockState state, final CallbackInfoReturnable<RenderShape> cir) {
        if (!ConfigCache.masterOptimize || !ConfigCache.optimizeChests) {
            return;
        }

        if (!AltRenderers.hasRendererOverride(BlockEntityType.ENDER_CHEST)) {
            cir.setReturnValue(RenderShape.MODEL);
        }
    }
}
