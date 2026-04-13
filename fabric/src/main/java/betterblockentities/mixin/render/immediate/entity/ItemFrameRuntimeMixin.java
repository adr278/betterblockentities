package betterblockentities.mixin.render.immediate.entity;

/* local */
import betterblockentities.client.chunk.pipeline.itemframe.ItemFrameEligibility;
import betterblockentities.client.render.immediate.entity.ItemFrameRuntimeHelper;

/* minecraft */
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.decoration.ItemFrame;

/* mixin */
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemFrame.class)
public abstract class ItemFrameRuntimeMixin {
    @Inject(method = "onSyncedDataUpdated", at = @At("TAIL"))
    private void markItemFrameDirty(EntityDataAccessor<?> dataAccessor, CallbackInfo ci) {
        if (!ItemFrameEligibility.optimizationEnabled()) return;
        if (ItemFrameRuntimeHelper.handlingPacketEntityDataUpdate()) return;

        ItemFrame frame = (ItemFrame) (Object) this;
        if (!(frame.level() instanceof ClientLevel clientLevel)) return;
        if (clientLevel.getEntity(frame.getId()) != frame) return;

        if (dataAccessor.equals(HangingEntityAccessor.getDataDirection())) {
            ItemFrameRuntimeHelper.onSupportPossiblyChanged(frame);
            return;
        }

        if (!dataAccessor.equals(ItemFrameAccessor.getDataItem())
                && !dataAccessor.equals(ItemFrameAccessor.getDataRotation())) return;
        ItemFrameRuntimeHelper.onFrameContentsChanged(frame);
    }
}
