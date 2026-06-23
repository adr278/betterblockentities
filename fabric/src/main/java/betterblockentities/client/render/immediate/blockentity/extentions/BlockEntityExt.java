package betterblockentities.client.render.immediate.blockentity.extentions;

import betterblockentities.client.render.immediate.blockentity.misc.RenderingMode;

public interface BlockEntityExt {
    boolean bbe$isSupportedBlockEntity();
    void bbe$setSupportedBlockEntity(boolean bl);

    RenderingMode bbe$getRenderingMode();
    void bbe$setRenderingMode(RenderingMode mode);

    boolean bbe$isTerrainMeshReady();
    void bbe$setTerrainMeshReady(boolean b);

    boolean bbe$hasSpecialManager();
    void bbe$setSpecialManager(boolean bl);

    byte bbe$getOptKind();
    void bbe$setOptKind(byte k);
}
