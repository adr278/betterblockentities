package betterblockentities.mixin.minecraft;

/* local */
import betterblockentities.util.BlockEntityManager;

/* minecraft */

/* mixin */
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockBehaviour.class)
public abstract class AbstractBlockMixin {
    /* apparently we do not need this? getRenderType always return type MODEL anyway... */
    @Inject(method = "getRenderShape", at = @At("HEAD"), cancellable = true)
    private void forceToMesh(BlockState state, CallbackInfoReturnable<RenderShape> cir) {
        if (BlockEntityManager.isSupportedBlock(state.getBlock()))
            cir.setReturnValue(RenderShape.MODEL);
    }
}
