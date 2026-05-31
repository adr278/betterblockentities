package betterblockentities.mixin.render.immediate.block;

/* local */
import betterblockentities.client.gui.config.ConfigCache;
import betterblockentities.render.AltRenderers;

/* minecraft */
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/* mixin */
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ShulkerBoxBlock.class)
public abstract class ShulkerBoxBlockMixin {
    @Inject(method = "getRenderShape", at = @At("HEAD"), cancellable = true)
    private void forceModelRenderShape(final BlockState state, final CallbackInfoReturnable<RenderShape> cir) {
        if (!ConfigCache.masterOptimize || !ConfigCache.optimizeShulker) {
            return;
        }

        if (!AltRenderers.hasRendererOverride(BlockEntityType.SHULKER_BOX)) {
            cir.setReturnValue(RenderShape.MODEL);
        }
    }
}
