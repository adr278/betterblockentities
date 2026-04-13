package betterblockentities.client.chunk.pipeline.itemframe;

/* local */
import betterblockentities.client.chunk.pipeline.shelf.CacheKeys;
import betterblockentities.client.chunk.pipeline.shelf.GeometryBaker;
import betterblockentities.client.chunk.pipeline.shelf.LongMeshCache;
import betterblockentities.client.chunk.pipeline.shelf.RenderTypeClassifier;
import betterblockentities.client.chunk.pipeline.shelf.SpriteRemapper;
import betterblockentities.mixin.render.immediate.blockentity.shelf.ItemStackRenderStateAccessor;
import betterblockentities.mixin.render.immediate.blockentity.shelf.ItemStackRenderStateLayerAccessor;

/* minecraft */
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/* java */
import java.util.ArrayList;

public final class ItemFrameItemModelBuilder {
    public record LayeredQuad(
            ChunkSectionLayer layer,
            int color,
            GeometryBaker.PackedQuad quad
    ) {}

    public record CapturedMesh(LayeredQuad[] quads) {
        public static final CapturedMesh EMPTY = new CapturedMesh(new LayeredQuad[0]);

        public boolean isEmpty() { return this.quads.length == 0; }
    }

    public record CaptureResult(CapturedMesh mesh, ItemFrameContentRenderMode contentRenderMode) {
        public static final CaptureResult EMPTY = new CaptureResult(CapturedMesh.EMPTY, ItemFrameContentRenderMode.NONE);
    }

    private static final RenderTypeClassifier RT = new RenderTypeClassifier();
    private static final SpriteRemapper SPRITES = new SpriteRemapper();
    private static final GeometryBaker BAKER = new GeometryBaker(RT, SPRITES);

    private static final int GLOBAL_CACHE_CAPACITY = 4096;
    private static final int GLOBAL_CACHE_SHARDS = 16;
    private static final int SHARD_CAPACITY = Math.max(1, GLOBAL_CACHE_CAPACITY / GLOBAL_CACHE_SHARDS);

    private static final LongMeshCache<CachedMesh>[] GLOBAL_MESH_CACHE = createShards();

    private static final ThreadLocal<ItemStackRenderState> RENDER_STATES =
            ThreadLocal.withInitial(ItemStackRenderState::new);

    public record CachedMesh(long sig0, long sig1, CaptureResult result) {}

    private ItemFrameItemModelBuilder() {}

    public static CaptureResult getOrCaptureMesh(ItemFrame frame, ItemStack stack) {
        if (stack.isEmpty()) return CaptureResult.EMPTY;

        int itemRawId = BuiltInRegistries.ITEM.getId(stack.getItem());
        Integer componentsHash = tryComponentsHash(stack);

        if (componentsHash == null) return bake(frame, stack);

        long sig0 = CacheKeys.packSig0(itemRawId, componentsHash);
        long sig1 = CacheKeys.packSig1(0);
        long key = CacheKeys.mix64(sig0, sig1);

        int shardIndex = shardIndex(key);
        LongMeshCache<CachedMesh> shard = GLOBAL_MESH_CACHE[shardIndex];

        synchronized (shard) {
            CachedMesh cached = shard.get(key);
            if (cached != null && cached.sig0 == sig0 && cached.sig1 == sig1) { return cached.result; }
        }

        CaptureResult baked = bake(frame, stack);

        synchronized (shard) {
            CachedMesh cached = shard.get(key);
            if (cached != null && cached.sig0 == sig0 && cached.sig1 == sig1) { return cached.result; }

            shard.put(key, new CachedMesh(sig0, sig1, baked));
            return baked;
        }
    }

    private static CaptureResult bake(ItemFrame frame, ItemStack stack) {
        Minecraft minecraft = Minecraft.getInstance();
        ItemModelResolver resolver = minecraft.getItemModelResolver();
        ItemStackRenderState state = RENDER_STATES.get();

        state.clear();
        resolver.updateForNonLiving(state, stack, ItemDisplayContext.FIXED, frame);

        if (state.isEmpty()) return new CaptureResult(CapturedMesh.EMPTY, ItemFrameContentRenderMode.IMMEDIATE_ITEM);
        if (hasNonSpecialFoilLayer(state)) return new CaptureResult(CapturedMesh.EMPTY, ItemFrameContentRenderMode.IMMEDIATE_ITEM);

        ArrayList<LayeredQuad> quads = new ArrayList<>(32);

        boolean supported = BAKER.captureItemGeometryFromResolvedState(
                state,
                (packedQuad, renderTypeObj, tintLayers) -> {
                    GeometryBaker.PackedQuad baked = BAKER.normalizeForCaching(packedQuad);
                    RenderTypeClassifier.Info info = RT.info(renderTypeObj);

                    int resolvedTint = CacheKeys.NO_TINT;
                    int tintIndex = baked.tintIndex();
                    if (tintIndex >= 0 && tintLayers != null && tintIndex < tintLayers.length) {
                        resolvedTint = tintLayers[tintIndex];
                    }

                    quads.add(new LayeredQuad(info.layer(), resolvedTint, baked));
                },
                true
        );

        if (!supported || quads.isEmpty()) return new CaptureResult(CapturedMesh.EMPTY, ItemFrameContentRenderMode.IMMEDIATE_ITEM);

        return new CaptureResult(new CapturedMesh(quads.toArray(new LayeredQuad[0])), ItemFrameContentRenderMode.NONE);
    }

    private static boolean hasNonSpecialFoilLayer(ItemStackRenderState state) {
        ItemStackRenderState.LayerRenderState[] layers = ((ItemStackRenderStateAccessor) state).getLayers();
        int activeLayerCount = ((ItemStackRenderStateAccessor) state).getActiveLayerCount();

        for (int i = 0; i < activeLayerCount; i++) {
            ItemStackRenderStateLayerAccessor layer = (ItemStackRenderStateLayerAccessor) layers[i];
            if (layer.getFoilType() != ItemStackRenderState.FoilType.NONE && layer.getSpecialRenderer() == null) {
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private static LongMeshCache<CachedMesh>[] createShards() {
        LongMeshCache<CachedMesh>[] shards = new LongMeshCache[GLOBAL_CACHE_SHARDS];
        for (int i = 0; i < GLOBAL_CACHE_SHARDS; i++) {
            shards[i] = new LongMeshCache<>(SHARD_CAPACITY);
        }
        return shards;
    }

    private static Integer tryComponentsHash(ItemStack stack) {
        try {
            return stack.getComponents().hashCode();
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static int shardIndex(long key) {
        return (int) (CacheKeys.mix64(key, key ^ 0x9E3779B97F4A7C15L) & (GLOBAL_CACHE_SHARDS - 1));
    }

    public static void invalidateAllCachesOnReload() {
        SPRITES.clear();
        RT.clear();

        for (LongMeshCache<CachedMesh> shard : GLOBAL_MESH_CACHE) {synchronized (shard) { shard.clear(); }}
    }
}
