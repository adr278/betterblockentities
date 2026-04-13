package betterblockentities.client.render.immediate.entity.extensions;

/* local */
import betterblockentities.client.chunk.pipeline.itemframe.ItemFrameContentRenderMode;

/* minecraft */
import net.minecraft.core.BlockPos;

/* annotations */
import org.jspecify.annotations.Nullable;

public interface ItemFrameExt {
    boolean terrainMeshReady();
    void terrainMeshReady(boolean ready);

    boolean terrainMeshActive();
    void terrainMeshActive(boolean active);

    BlockPos lastSupportPos();
    void lastSupportPos(BlockPos pos);

    ItemFrameContentRenderMode contentRenderMode();
    void contentRenderMode(ItemFrameContentRenderMode mode);

    boolean cachedItemStateValid();
    void cachedItemStateValid(boolean valid);

    int cachedItemRawId();
    void cachedItemRawId(int rawId);

    int cachedComponentsHash();
    void cachedComponentsHash(int hash);

    long terrainStateRefreshGameTime();
    void terrainStateRefreshGameTime(long gameTime);

    @Nullable BlockPos terrainStateRefreshSupportPos();
    void terrainStateRefreshSupportPos(@Nullable BlockPos supportPos);
}
