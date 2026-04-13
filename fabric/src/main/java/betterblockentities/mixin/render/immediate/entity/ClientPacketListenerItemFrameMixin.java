package betterblockentities.mixin.render.immediate.entity;

/* local */
import betterblockentities.client.chunk.pipeline.itemframe.ItemFrameEligibility;
import betterblockentities.client.render.immediate.entity.ItemFrameRuntimeHelper;

/* minecraft */
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundMapItemDataPacket;

/* mixin */
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerItemFrameMixin {
    @Shadow private ClientLevel level;

    @Inject(method = "handleMapItemData", at = @At("TAIL"))
    private void refreshMapsAfterMapPacket(ClientboundMapItemDataPacket packet, CallbackInfo ci) {
        if (!ItemFrameEligibility.optimizationEnabled()) return;

        if (this.level != null) {
            ItemFrameRuntimeHelper.onMapDataUpdated(packet.mapId(), packet.colorPatch().isPresent());
        }
    }
}
