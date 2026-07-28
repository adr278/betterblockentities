package betterblockentities.mixin.render.immediate.blockentity.extentions;

/* local */
import betterblockentities.client.render.immediate.blockentity.extentions.BlockEntityExt;
import betterblockentities.client.render.immediate.blockentity.misc.RenderingMode;

/* minecraft */
import net.minecraft.world.level.block.entity.BlockEntity;

/* mixin */
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(BlockEntity.class)
public abstract class BlockEntityMixin implements BlockEntityExt {
    @Unique private RenderingMode renderingMode = RenderingMode.TERRAIN;
    @Unique private boolean terrainMeshReady = true;
    @Unique private boolean hasSpecialManager = false;
    @Unique private boolean breakingOverlay = false;
    @Unique private byte bbeKind = 0;
    @Unique private boolean supportedBlockEntity = false;

    @Override public boolean bbe$isSupportedBlockEntity() { return supportedBlockEntity; }
    @Override public void bbe$setSupportedBlockEntity(boolean bl) {this.supportedBlockEntity = bl; }

    @Override public RenderingMode bbe$getRenderingMode() { return renderingMode; }
    @Override public void bbe$setRenderingMode(RenderingMode mode) { renderingMode = mode; }

    @Override public boolean bbe$isTerrainMeshReady() { return terrainMeshReady; }
    @Override public void bbe$setTerrainMeshReady(boolean bl) { terrainMeshReady = bl; }

    @Override public boolean bbe$hasSpecialManager() { return hasSpecialManager; }
    @Override public void bbe$setSpecialManager(boolean bl) { hasSpecialManager = bl; }

    @Override public boolean bbe$hasBreakingOverlay() { return breakingOverlay; }
    @Override public void bbe$setBreakingOverlay(boolean bl) { breakingOverlay = bl; }

    @Override public byte bbe$getOptKind() { return bbeKind; }
    @Override public void bbe$setOptKind(byte k) { bbeKind = k; }
}