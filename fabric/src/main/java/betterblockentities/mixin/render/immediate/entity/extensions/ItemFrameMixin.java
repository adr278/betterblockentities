package betterblockentities.mixin.render.immediate.entity.extensions;

/* local */
import betterblockentities.client.chunk.pipeline.itemframe.ItemFrameContentRenderMode;
import betterblockentities.client.chunk.pipeline.itemframe.MapLifecycleState;
import betterblockentities.client.render.immediate.entity.extensions.ItemFrameExt;

/* minecraft */
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.level.saveddata.maps.MapId;

/* annotations */
import org.jspecify.annotations.Nullable;

/* mixin */
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ItemFrame.class)
public class ItemFrameMixin implements ItemFrameExt {
    @Unique private boolean terrainMeshReady;
    @Unique private boolean terrainMeshActive;
    @Unique private boolean renderImmediateWhileWaiting = true;
    @Unique private BlockPos lastSupportPos = BlockPos.ZERO;
    @Unique private int missingPayloadRetryCount;
    @Unique private ItemFrameContentRenderMode contentRenderMode = ItemFrameContentRenderMode.NONE;
    @Unique private boolean latchedTerrainSnapshot = false;
    @Unique private ItemFrameContentRenderMode latchedContentRenderMode = ItemFrameContentRenderMode.NONE;
    @Unique private MapLifecycleState mapLifecycleState = MapLifecycleState.NONE;
    @Unique private boolean cachedItemStateValid;
    @Unique private @Nullable MapId cachedFramedMapId;
    @Unique private int cachedItemRawId;
    @Unique private int cachedComponentsHash;
    @Unique private long uploadedStateRefreshGameTime = Long.MIN_VALUE;
    @Unique private @Nullable BlockPos uploadedStateRefreshSupportPos;
    @Unique private int fallbackMapLight;
    @Unique private long fallbackMapLightGameTime = Long.MIN_VALUE;
    @Unique private @Nullable BlockPos fallbackMapLightSupportPos;

    @Override public boolean terrainMeshReady() { return this.terrainMeshReady; }
    @Override public void terrainMeshReady(boolean ready) { this.terrainMeshReady = ready; }

    @Override public boolean terrainMeshActive() { return this.terrainMeshActive; }
    @Override public void terrainMeshActive(boolean active) { this.terrainMeshActive = active; }

    @Override public boolean renderImmediateWhileWaiting() { return this.renderImmediateWhileWaiting; }
    @Override public void renderImmediateWhileWaiting(boolean renderImmediate) { this.renderImmediateWhileWaiting = renderImmediate; }

    @Override public boolean latchedTerrainSnapshot() { return this.latchedTerrainSnapshot; }
    @Override public void latchedTerrainSnapshot(boolean latched) { this.latchedTerrainSnapshot = latched; }

    @Override public ItemFrameContentRenderMode latchedContentRenderMode() { return this.latchedContentRenderMode; }
    @Override public void latchedContentRenderMode(ItemFrameContentRenderMode mode) { this.latchedContentRenderMode = mode; }

    @Override public BlockPos lastSupportPos() { return this.lastSupportPos; }
    @Override public void lastSupportPos(BlockPos pos) { this.lastSupportPos = pos; }

    @Override public int missingPayloadRetryCount() { return this.missingPayloadRetryCount; }
    @Override public void missingPayloadRetryCount(int retryCount) { this.missingPayloadRetryCount = retryCount; }

    @Override public ItemFrameContentRenderMode contentRenderMode() { return this.contentRenderMode; }
    @Override public void contentRenderMode(ItemFrameContentRenderMode mode) { this.contentRenderMode = mode; }

    @Override public MapLifecycleState mapLifecycleState() { return this.mapLifecycleState; }
    @Override public void mapLifecycleState(MapLifecycleState state) { this.mapLifecycleState = state; }

    @Override public boolean cachedItemStateValid() { return this.cachedItemStateValid; }
    @Override public void cachedItemStateValid(boolean valid) { this.cachedItemStateValid = valid; }

    @Override public @Nullable MapId cachedFramedMapId() { return this.cachedFramedMapId; }
    @Override public void cachedFramedMapId(@Nullable MapId mapId) { this.cachedFramedMapId = mapId; }

    @Override public int cachedItemRawId() { return this.cachedItemRawId; }
    @Override public void cachedItemRawId(int rawId) { this.cachedItemRawId = rawId; }

    @Override public int cachedComponentsHash() { return this.cachedComponentsHash; }
    @Override public void cachedComponentsHash(int hash) { this.cachedComponentsHash = hash; }

    @Override public long uploadedStateRefreshGameTime() { return this.uploadedStateRefreshGameTime; }
    @Override public void uploadedStateRefreshGameTime(long gameTime) { this.uploadedStateRefreshGameTime = gameTime; }

    @Override public @Nullable BlockPos uploadedStateRefreshSupportPos() { return this.uploadedStateRefreshSupportPos; }
    @Override public void uploadedStateRefreshSupportPos(@Nullable BlockPos supportPos) { this.uploadedStateRefreshSupportPos = supportPos; }

    @Override public int fallbackMapLight() { return this.fallbackMapLight; }
    @Override public void fallbackMapLight(int light) { this.fallbackMapLight = light; }

    @Override public long fallbackMapLightGameTime() { return this.fallbackMapLightGameTime; }
    @Override public void fallbackMapLightGameTime(long gameTime) { this.fallbackMapLightGameTime = gameTime; }

    @Override public @Nullable BlockPos fallbackMapLightSupportPos() { return this.fallbackMapLightSupportPos; }
    @Override public void fallbackMapLightSupportPos(@Nullable BlockPos supportPos) { this.fallbackMapLightSupportPos = supportPos; }
}
