package betterblockentities.mixin.render.immediate.entity;

/* local */
import betterblockentities.client.chunk.pipeline.itemframe.ItemFrameEligibility;
import betterblockentities.client.render.immediate.entity.ItemFrameRuntimeHelper;

/* minecraft */
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

/* mixin */
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/* java */
import java.util.Map;

@Mixin(ClientLevel.class)
public abstract class ClientLevelItemFrameMixin {
    @Inject(method = "addEntity", at = @At("TAIL"))
    private void trackAddedItemFrames(Entity entity, CallbackInfo ci) {
        if (!ItemFrameEligibility.optimizationEnabled()) return;

        if (entity instanceof ItemFrame frame) ItemFrameRuntimeHelper.onAdded(frame);
    }

    @Inject(method = "removeEntity", at = @At("HEAD"))
    private void trackRemovedItemFrames(int id, Entity.RemovalReason reason, CallbackInfo ci) {
        if (!ItemFrameEligibility.optimizationEnabled()) return;

        Entity entity = ((ClientLevel) (Object) this).getEntity(id);
        if (entity instanceof ItemFrame frame) ItemFrameRuntimeHelper.onRemoved(frame);
    }

    @Inject(method = "overrideMapData", at = @At("TAIL"))
    private void refreshMaps(MapId mapId, MapItemSavedData data, CallbackInfo ci) {
        if (!ItemFrameEligibility.optimizationEnabled()) return;

        ItemFrameRuntimeHelper.onMapDataUpdated(mapId);
    }

    @Inject(method = "addMapData", at = @At("TAIL"))
    private void refreshBulkMapData(Map<MapId, MapItemSavedData> mapData, CallbackInfo ci) {
        if (!ItemFrameEligibility.optimizationEnabled()) return;

        ClientLevel level = (ClientLevel) (Object) this;
        for (MapId mapId : mapData.keySet()) ItemFrameRuntimeHelper.onMapDataUpdated(mapId);
    }
}
