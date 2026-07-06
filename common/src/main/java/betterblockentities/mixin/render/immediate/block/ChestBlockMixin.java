package betterblockentities.mixin.render.immediate.block;

/* local */
import betterblockentities.client.gui.config.ConfigCache;
import betterblockentities.client.render.immediate.util.VanillaBlockSupport;
import betterblockentities.platform.GlobalScope;
import betterblockentities.render.AltRenderers;

/* minecraft */
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/* mixin */
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChestBlock.class)
public abstract class ChestBlockMixin {
    @Inject(method = "getRenderShape", at = @At("HEAD"), cancellable = true)
    private void forceModelRenderShape(final BlockState blockState, final CallbackInfoReturnable<RenderShape> cir) {
        if (!ConfigCache.masterOptimize || !ConfigCache.optimizeChests) {
            return;
        }

        if (GlobalScope.isRenderingMinecartDisplay) {
            return;
        }

        if (!VanillaBlockSupport.isVanillaChestBlock(blockState)) {
            return;
        }

        final BlockEntityType<?> type = blockState.is(Blocks.TRAPPED_CHEST) ? BlockEntityType.TRAPPED_CHEST : BlockEntityType.CHEST;

        if (!AltRenderers.hasRendererOverride(type)) {
            cir.setReturnValue(RenderShape.MODEL);
        }
    }
}
