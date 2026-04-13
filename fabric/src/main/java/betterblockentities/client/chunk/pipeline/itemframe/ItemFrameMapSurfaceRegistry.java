package betterblockentities.client.chunk.pipeline.itemframe;

/* minecraft */
import net.minecraft.world.level.saveddata.maps.MapId;

/* java */
import java.util.Arrays;

/* annotations */
import org.jspecify.annotations.Nullable;

public final class ItemFrameMapSurfaceRegistry {
    private static final int PAGE_BITS = 8;
    private static final int PAGE_SIZE = 1 << PAGE_BITS;
    private static final int PAGE_MASK = PAGE_SIZE - 1;

    public static final class ActiveSurfaceState {
        private MapId mapId;
        private int mapLight;

        private ActiveSurfaceState(MapId mapId, int mapLight) {
            this.mapId = mapId;
            this.mapLight = mapLight;
        }

        public MapId mapId() { return this.mapId; }

        public int mapLight() { return this.mapLight; }

        public void update(MapId mapId, int mapLight) {
            this.mapId = mapId;
            this.mapLight = mapLight;
        }
    }

    private static ActiveSurfaceState[][] ACTIVE_SURFACE_PAGES = new ActiveSurfaceState[16][];
    private static int VERSION = 1;

    private ItemFrameMapSurfaceRegistry() {}

    public static @Nullable ActiveSurfaceState get(int entityId) {
        if (entityId < 0) return null;

        int pageIndex = entityId >>> PAGE_BITS;
        if (pageIndex >= ACTIVE_SURFACE_PAGES.length) return null;

        ActiveSurfaceState[] page = ACTIVE_SURFACE_PAGES[pageIndex];
        return page != null ? page[entityId & PAGE_MASK] : null;
    }

    public static int version() { return VERSION; }

    public static void activate(int entityId, MapId mapId, int mapLight) {
        if (entityId < 0) return;

        int pageIndex = entityId >>> PAGE_BITS;
        ensurePageCapacity(pageIndex);

        ActiveSurfaceState[] page = ACTIVE_SURFACE_PAGES[pageIndex];
        if (page == null) {
            page = new ActiveSurfaceState[PAGE_SIZE];
            ACTIVE_SURFACE_PAGES[pageIndex] = page;
        }

        int slot = entityId & PAGE_MASK;
        ActiveSurfaceState state = page[slot];
        if (state == null) {
            page[slot] = new ActiveSurfaceState(mapId, mapLight);
            bumpVersion();
            return;
        }

        if (!state.mapId().equals(mapId) || state.mapLight() != mapLight) {
            state.update(mapId, mapLight);
            bumpVersion();
        }
    }

    public static void deactivate(int entityId) {
        if (entityId < 0) return;

        int pageIndex = entityId >>> PAGE_BITS;
        if (pageIndex >= ACTIVE_SURFACE_PAGES.length) return;

        ActiveSurfaceState[] page = ACTIVE_SURFACE_PAGES[pageIndex];
        if (page == null) return;

        int slot = entityId & PAGE_MASK;
        if (page[slot] == null) return;

        page[slot] = null;
        bumpVersion();
    }

    public static void clear() {
        Arrays.fill(ACTIVE_SURFACE_PAGES, null);
        bumpVersion();
    }

    private static void ensurePageCapacity(int pageIndex) {
        if (pageIndex < ACTIVE_SURFACE_PAGES.length) return;

        int newLength = ACTIVE_SURFACE_PAGES.length;
        while (pageIndex >= newLength) {
            newLength *= 2;
        }

        ACTIVE_SURFACE_PAGES = Arrays.copyOf(ACTIVE_SURFACE_PAGES, newLength);
    }

    private static void bumpVersion() { VERSION = VERSION == Integer.MAX_VALUE ? 1 : VERSION + 1; }
}
