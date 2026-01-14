package betterblockentities.mixin.render.immediate.blockentity.decordatedpot;

/* local */
import betterblockentities.client.render.immediate.blockentity.BlockEntityExt;

/* minecraft */
import net.minecraft.world.level.block.entity.DecoratedPotBlockEntity;

/* mixin */
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DecoratedPotBlockEntity.class)
public class DecoratedPotBlockEntityMixin {
    /* capture block event for conditional rendering in BlockEntityManager */
    @Inject(method = "triggerEvent", at = @At(value = "RETURN", shift = At.Shift.BEFORE, ordinal = 0))
    private void onBlockEvent(int type, int data, CallbackInfoReturnable<Boolean> cir) {
        ((BlockEntityExt)this).setJustReceivedUpdate(true);
    }
}
