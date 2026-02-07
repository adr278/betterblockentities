package betterblockentities.client.render.immediate.blockentity;

@SuppressWarnings("unused")
public interface ShelfSlotMeshCacheAccess {
    Object getSlotMesh(int slot, int epoch, long sig0, long sig1);
    void putSlotMesh(int slot, int epoch, long sig0, long sig1, Object mesh);
    void clearSlotMesh(int slot);
    void clearSlotMeshes();
}
