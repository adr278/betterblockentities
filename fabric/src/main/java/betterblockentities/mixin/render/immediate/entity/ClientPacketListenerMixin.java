package betterblockentities.mixin.render.immediate.entity;

/* local */
import betterblockentities.client.chunk.pipeline.itemframe.ItemFrameEligibility;
import betterblockentities.client.render.immediate.entity.ItemFrameRuntimeHelper;

/* minecraft */
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundEntityPositionSyncPacket;
import net.minecraft.network.protocol.game.ClientboundMoveEntityPacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ItemFrame;

/* mixin */
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin {
    @Shadow private ClientLevel level;

    @Inject(
            method = "handleAddEntity",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/multiplayer/ClientLevel;addEntity(Lnet/minecraft/world/entity/Entity;)V",
                    shift = At.Shift.AFTER
            )
    )
    private void trackHandleAddEntity(ClientboundAddEntityPacket packet, CallbackInfo ci) {
        if (!ItemFrameEligibility.optimizationEnabled()) return;
        if (this.level == null) return;

        Entity entity = this.level.getEntity(packet.getId());
        if (entity instanceof ItemFrame frame) ItemFrameRuntimeHelper.onAdded(frame);
    }

    @Inject(method = "handleSetEntityData", at = @At("HEAD"))
    private void beginItemFrameDataPacketPhase(ClientboundSetEntityDataPacket packet, CallbackInfo ci) {
        ItemFrameRuntimeHelper.beginPacketEntityDataUpdate();
    }

    @Inject(method = "handleSetEntityData", at = @At("TAIL"))
    private void trackHandleSetEntityData(ClientboundSetEntityDataPacket packet, CallbackInfo ci) {
        if (!ItemFrameEligibility.optimizationEnabled()) return;
        if (this.level == null) return;

        Entity entity = this.level.getEntity(packet.id());
        if (!(entity instanceof ItemFrame frame)) return;

        ItemFrameRuntimeHelper.onSupportPossiblyChanged(frame);
        ItemFrameRuntimeHelper.onFrameContentsChanged(frame);
    }

    @Inject(method = "handleSetEntityData", at = @At("RETURN"))
    private void finishItemFrameDataPacketPhase(ClientboundSetEntityDataPacket packet, CallbackInfo ci) {
        ItemFrameRuntimeHelper.endPacketEntityDataUpdate();
    }

    @Inject(method = "handleEntityPositionSync", at = @At("RETURN"))
    private void trackHandleEntityPositionSync(ClientboundEntityPositionSyncPacket packet, CallbackInfo ci) {
        if (!ItemFrameEligibility.optimizationEnabled()) return;
        if (this.level == null) return;

        Entity entity = this.level.getEntity(packet.id());
        if (entity instanceof ItemFrame frame) ItemFrameRuntimeHelper.onSupportPossiblyChanged(frame);
    }

    @Inject(method = "handleTeleportEntity", at = @At("RETURN"))
    private void trackHandleTeleportEntity(ClientboundTeleportEntityPacket packet, CallbackInfo ci) {
        if (!ItemFrameEligibility.optimizationEnabled()) return;
        if (this.level == null) return;

        Entity entity = this.level.getEntity(packet.id());
        if (entity instanceof ItemFrame frame) ItemFrameRuntimeHelper.onSupportPossiblyChanged(frame);
    }

    @Inject(method = "handleMoveEntity", at = @At("RETURN"))
    private void trackHandleMoveEntity(ClientboundMoveEntityPacket packet, CallbackInfo ci) {
        if (!ItemFrameEligibility.optimizationEnabled()) return;
        if (this.level == null) return;

        Entity entity = packet.getEntity(this.level);
        if (entity instanceof ItemFrame frame) ItemFrameRuntimeHelper.onSupportPossiblyChanged(frame);
    }

    @Inject(method = "handleRemoveEntities", at = @At("HEAD"))
    private void trackHandleRemoveEntities(ClientboundRemoveEntitiesPacket packet, CallbackInfo ci) {
        ItemFrameRuntimeHelper.beginPacketEntityRemoval();

        if (!ItemFrameEligibility.optimizationEnabled()) return;
        if (this.level == null) return;

        packet.getEntityIds().forEach(i -> {
            Entity entity = this.level.getEntity(i);
            if (entity instanceof ItemFrame frame) ItemFrameRuntimeHelper.onRemoved(frame);
        });
    }

    @Inject(method = "handleRemoveEntities", at = @At("RETURN"))
    private void finishItemFrameRemovalPacketPhase(ClientboundRemoveEntitiesPacket packet, CallbackInfo ci) {
        ItemFrameRuntimeHelper.endPacketEntityRemoval();
    }
}
