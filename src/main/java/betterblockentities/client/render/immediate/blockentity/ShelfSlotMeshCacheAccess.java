package betterblockentities.client.render.immediate.blockentity;

public interface ShelfSlotMeshCacheAccess {
    Object getSlotMesh(int slot, int epoch, long sig0, long sig1);

    void putSlotMesh(int slot, int epoch, long sig0, long sig1, Object mesh);

    void clearSlotMeshes();
}
