package betterblockentities.client.render.immediate.entity.extensions;

/* local */
import betterblockentities.client.chunk.pipeline.itemframe.ItemFrameContentRenderMode;
import betterblockentities.client.chunk.pipeline.itemframe.MapLifecycleState;

/* minecraft */
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.saveddata.maps.MapId;

/* annotations */
import org.jspecify.annotations.Nullable;

public interface ItemFrameExt {
    boolean terrainMeshReady();
    void terrainMeshReady(boolean ready);

    boolean terrainMeshActive();
    void terrainMeshActive(boolean active);

    boolean renderImmediateWhileWaiting();
    void renderImmediateWhileWaiting(boolean renderImmediate);

    boolean latchedTerrainSnapshot();
    void latchedTerrainSnapshot(boolean latched);

    ItemFrameContentRenderMode latchedContentRenderMode();
    void latchedContentRenderMode(ItemFrameContentRenderMode mode);

    BlockPos lastSupportPos();
    void lastSupportPos(BlockPos pos);

    int missingPayloadRetryCount();
    void missingPayloadRetryCount(int retryCount);

    ItemFrameContentRenderMode contentRenderMode();
    void contentRenderMode(ItemFrameContentRenderMode mode);

    MapLifecycleState mapLifecycleState();
    void mapLifecycleState(MapLifecycleState state);

    boolean cachedItemStateValid();
    void cachedItemStateValid(boolean valid);

    @Nullable MapId cachedFramedMapId();
    void cachedFramedMapId(@Nullable MapId mapId);

    int cachedItemRawId();
    void cachedItemRawId(int rawId);

    int cachedComponentsHash();
    void cachedComponentsHash(int hash);

    long uploadedStateRefreshGameTime();
    void uploadedStateRefreshGameTime(long gameTime);

    @Nullable BlockPos uploadedStateRefreshSupportPos();
    void uploadedStateRefreshSupportPos(@Nullable BlockPos supportPos);

    int fallbackMapLight();
    void fallbackMapLight(int light);

    long fallbackMapLightGameTime();
    void fallbackMapLightGameTime(long gameTime);

    @Nullable BlockPos fallbackMapLightSupportPos();
    void fallbackMapLightSupportPos(@Nullable BlockPos supportPos);
}
