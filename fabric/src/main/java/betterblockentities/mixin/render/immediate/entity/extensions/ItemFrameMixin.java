package betterblockentities.mixin.render.immediate.entity.extensions;

/* local */
import betterblockentities.client.chunk.pipeline.itemframe.ItemFrameContentRenderMode;
import betterblockentities.client.render.immediate.entity.extensions.ItemFrameExt;

/* minecraft */
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.decoration.ItemFrame;

/* annotations */
import org.jspecify.annotations.Nullable;

/* mixin */
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ItemFrame.class)
public class ItemFrameMixin implements ItemFrameExt {
    @Unique private boolean terrainMeshReady;
    @Unique private boolean terrainMeshActive;
    @Unique private BlockPos lastSupportPos = BlockPos.ZERO;
    @Unique private ItemFrameContentRenderMode contentRenderMode = ItemFrameContentRenderMode.NONE;
    @Unique private boolean cachedItemStateValid;
    @Unique private int cachedItemRawId;
    @Unique private int cachedComponentsHash;
    @Unique private long terrainStateRefreshGameTime = Long.MIN_VALUE;
    @Unique private @Nullable BlockPos terrainStateRefreshSupportPos;

    @Override public boolean terrainMeshReady() { return this.terrainMeshReady; }
    @Override public void terrainMeshReady(boolean ready) { this.terrainMeshReady = ready; }

    @Override public boolean terrainMeshActive() { return this.terrainMeshActive; }
    @Override public void terrainMeshActive(boolean active) { this.terrainMeshActive = active; }

    @Override public BlockPos lastSupportPos() { return this.lastSupportPos; }
    @Override public void lastSupportPos(BlockPos pos) { this.lastSupportPos = pos; }

    @Override public ItemFrameContentRenderMode contentRenderMode() { return this.contentRenderMode; }
    @Override public void contentRenderMode(ItemFrameContentRenderMode mode) { this.contentRenderMode = mode; }

    @Override public boolean cachedItemStateValid() { return this.cachedItemStateValid; }
    @Override public void cachedItemStateValid(boolean valid) { this.cachedItemStateValid = valid; }

    @Override public int cachedItemRawId() { return this.cachedItemRawId; }
    @Override public void cachedItemRawId(int rawId) { this.cachedItemRawId = rawId; }

    @Override public int cachedComponentsHash() { return this.cachedComponentsHash; }
    @Override public void cachedComponentsHash(int hash) { this.cachedComponentsHash = hash; }

    @Override public long terrainStateRefreshGameTime() { return this.terrainStateRefreshGameTime; }
    @Override public void terrainStateRefreshGameTime(long gameTime) { this.terrainStateRefreshGameTime = gameTime; }

    @Override public @Nullable BlockPos terrainStateRefreshSupportPos() { return this.terrainStateRefreshSupportPos; }
    @Override public void terrainStateRefreshSupportPos(@Nullable BlockPos supportPos) { this.terrainStateRefreshSupportPos = supportPos; }
}
