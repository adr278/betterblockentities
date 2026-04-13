package betterblockentities.mixin.render.immediate.entity;

/* local */
import betterblockentities.client.chunk.pipeline.itemframe.ItemFrameEligibility;
import betterblockentities.client.render.immediate.entity.ItemFrameRuntimeHelper;

/* minecraft */
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.entity.decoration.ItemFrame;

/* mixin */
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HangingEntity.class)
public abstract class HangingEntityItemFrameMixin {
    @Inject(method = "onSyncedDataUpdated", at = @At("TAIL"))
    private void markSupportDirty(EntityDataAccessor<?> dataAccessor, CallbackInfo ci) {
        if (!ItemFrameEligibility.optimizationEnabled()) return;

        if (!dataAccessor.equals(HangingEntityAccessor.getDataDirection())) return;
        if ((Object) this instanceof ItemFrame frame) ItemFrameRuntimeHelper.onSupportPossiblyChanged(frame);
    }
}
