package betterblockentities.client.chunk.pipeline.itemframe;

/* local */
import betterblockentities.mixin.render.immediate.entity.MapRendererAccessor;

/* minecraft */
import net.minecraft.client.renderer.MapRenderer;
import net.minecraft.client.renderer.state.MapRenderState;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.saveddata.maps.MapDecoration;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

/* java */
import java.util.ArrayList;
import java.util.Collections;

public final class ItemFrameFallbackMapRenderStateCache {
    private static final int PAGE_BITS = 8;
    private static final int PAGE_SIZE = 1 << PAGE_BITS;
    private static final int PAGE_MASK = PAGE_SIZE - 1;

    private static CachedState[][] cachedStates = new CachedState[16][];

    private static final class CachedState {
        private Identifier texture;
        private MapRenderState.MapDecorationRenderState[] decorations;
        private MapItemSavedData mapData;
        private boolean textureDirty;
        private boolean decorationsDirty;

        private CachedState(
                Identifier texture,
                MapRenderState.MapDecorationRenderState[] decorations,
                MapItemSavedData mapData
        ) {
            this.texture = texture;
            this.decorations = decorations;
            this.mapData = mapData;
        }
    }

    private ItemFrameFallbackMapRenderStateCache() {}

    public static void extract(
            MapRenderer mapRenderer,
            MapId mapId,
            MapItemSavedData mapData,
            MapRenderState state
    ) {
        int numericMapId = mapId.id();
        CachedState cached = cachedState(numericMapId);
        if (cached != null && MapAtlasManager.ATLAS_TEXTURE.equals(cached.texture)) {
            cached.textureDirty = true;
            cached.decorationsDirty = true;
        }

        // Flashback/replay paths can swap the saved data object without a normal map packet.
        // Vanilla re-uploads on object replacement, so mirror that invalidation here.
        if (cached != null && cached.mapData != mapData) {
            cached.textureDirty = true;
            cached.decorationsDirty = true;
        }

        if (cached != null && !cached.textureDirty && !cached.decorationsDirty) {
            apply(cached, state);
            return;
        }

        if (cached != null && !cached.textureDirty) {
            refreshDecorations(mapRenderer, mapData, cached);
            apply(cached, state);
            return;
        }

        state.texture = null;
        state.decorations.clear();
        mapRenderer.extractRenderState(mapId, mapData, state);

        if (state.texture != null && !MapAtlasManager.ATLAS_TEXTURE.equals(state.texture)) {
            cache(numericMapId, state, mapData);
        }
    }

    public static void markDirty(MapId mapId, boolean textureDirty) {
        CachedState cached = cachedState(mapId.id());
        if (cached == null) return;

        cached.textureDirty |= textureDirty;
        cached.decorationsDirty = true;
    }

    public static void clear() {
        cachedStates = new CachedState[16][];
    }

    private static void apply(CachedState cached, MapRenderState state) {
        state.texture = cached.texture;
        state.decorations.clear();
        Collections.addAll(state.decorations, cached.decorations);
    }

    private static void cache(int mapId, MapRenderState state, MapItemSavedData mapData) {
        if (mapId < 0) return;

        int pageIndex = mapId >>> PAGE_BITS;
        ensurePageCapacity(pageIndex);

        CachedState[] page = cachedStates[pageIndex];
        if (page == null) {
            page = new CachedState[PAGE_SIZE];
            cachedStates[pageIndex] = page;
        }

        int entryIndex = mapId & PAGE_MASK;
        MapRenderState.MapDecorationRenderState[] decorations =
                state.decorations.toArray(MapRenderState.MapDecorationRenderState[]::new);
        CachedState cached = page[entryIndex];
        if (cached == null) {
            page[entryIndex] = new CachedState(state.texture, decorations, mapData);
            return;
        }

        cached.texture = state.texture;
        cached.decorations = decorations;
        cached.mapData = mapData;
        cached.textureDirty = false;
        cached.decorationsDirty = false;
    }

    private static void refreshDecorations(
            MapRenderer mapRenderer,
            MapItemSavedData mapData,
            CachedState cached
    ) {
        TextureAtlas decorationSprites = ((MapRendererAccessor) mapRenderer).getDecorationSprites();
        ArrayList<MapRenderState.MapDecorationRenderState> decorations = new ArrayList<>();

        for (MapDecoration decoration : mapData.getDecorations()) {
            MapRenderState.MapDecorationRenderState state = new MapRenderState.MapDecorationRenderState();
            state.atlasSprite = decorationSprites.getSprite(decoration.getSpriteLocation());
            state.x = decoration.x();
            state.y = decoration.y();
            state.rot = decoration.rot();
            state.name = decoration.name().orElse(null);
            state.renderOnFrame = decoration.renderOnFrame();
            decorations.add(state);
        }

        cached.decorations = decorations.toArray(MapRenderState.MapDecorationRenderState[]::new);
        cached.decorationsDirty = false;
    }

    private static CachedState cachedState(int mapId) {
        if (mapId < 0) return null;

        int pageIndex = mapId >>> PAGE_BITS;
        if (pageIndex >= cachedStates.length) return null;

        CachedState[] page = cachedStates[pageIndex];
        return page != null ? page[mapId & PAGE_MASK] : null;
    }

    private static void ensurePageCapacity(int pageIndex) {
        if (pageIndex < cachedStates.length) return;

        int newLength = cachedStates.length;
        while (pageIndex >= newLength) {
            newLength *= 2;
        }

        CachedState[][] expanded = new CachedState[newLength][];
        System.arraycopy(cachedStates, 0, expanded, 0, cachedStates.length);
        cachedStates = expanded;
    }
}
