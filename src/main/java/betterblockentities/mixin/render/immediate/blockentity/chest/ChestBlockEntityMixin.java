package betterblockentities.mixin.render.immediate.blockentity.chest;

/* local */
import betterblockentities.client.render.immediate.blockentity.BlockEntityExt;

/* minecraft */
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/* mixin */
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChestBlockEntity.class)
public abstract class ChestBlockEntityMixin {
    /* only run tick logic when we receive a block event */
    @Inject(method = "lidAnimateTick", at = @At("HEAD"), cancellable = true)
    private static void onTick(Level level, BlockPos blockPos, BlockState blockState, ChestBlockEntity blockEntity, CallbackInfo ci) {
        if (!(((BlockEntityExt)blockEntity).getJustReceivedUpdate()))
            ci.cancel();
    }

    /* capture block event for conditional rendering in BlockEntityManager */
    @Inject(method = "triggerEvent", at = @At("HEAD"), cancellable = true)
    private void onBlockEvent(int type, int data, CallbackInfoReturnable<Boolean> cir) {
        if (type != 1) return;
        ((BlockEntityExt)this).setJustReceivedUpdate(true);
    }
}
