package betterblockentities.client.chunk.pipeline.itemframe;

import java.util.concurrent.ConcurrentHashMap;

public final class ItemFrameRemovalTracker {
    private static final Boolean PRESENT = Boolean.TRUE;
    private static final ConcurrentHashMap<Integer, Boolean> REMOVED_FRAME_IDS = new ConcurrentHashMap<>();

    private ItemFrameRemovalTracker() {}

    public static void markAdded(int entityId) {
        // Avoid a ConcurrentHashMap remove on every healthy indexed frame.
        if (!REMOVED_FRAME_IDS.isEmpty() && REMOVED_FRAME_IDS.get(entityId) != null) {
            REMOVED_FRAME_IDS.remove(entityId);
        }
    }

    public static void clearIfRemoved(int entityId) {
        if (REMOVED_FRAME_IDS.isEmpty()) return;
        REMOVED_FRAME_IDS.remove(entityId);
    }

    public static void markRemoved(int entityId) { REMOVED_FRAME_IDS.put(entityId, PRESENT); }

    public static boolean isRemoved(int entityId) { return !REMOVED_FRAME_IDS.isEmpty() && REMOVED_FRAME_IDS.containsKey(entityId); }

    public static void clear() { REMOVED_FRAME_IDS.clear(); }
}
