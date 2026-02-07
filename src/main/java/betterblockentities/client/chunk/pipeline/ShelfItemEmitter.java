package betterblockentities.client.chunk.pipeline;

/* local */
import betterblockentities.client.render.immediate.blockentity.ShelfSlotMeshCacheAccess;
import betterblockentities.client.model.geometry.RecordingVertexConsumer;
import betterblockentities.mixin.render.immediate.blockentity.shelf.*;

/* minecraft */
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.feature.CustomFeatureRenderer;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.feature.ModelPartFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ShelfBlock;
import net.minecraft.world.level.block.entity.ShelfBlockEntity;
import net.minecraft.world.phys.AABB;

/* sodium */
import net.caffeinemc.mods.sodium.client.render.model.MutableQuadViewImpl;
import net.caffeinemc.mods.sodium.client.render.model.SodiumQuadAtlas;

/* java/misc */
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public final class ShelfItemEmitter {
    private ShelfItemEmitter() {}

    private static final ThreadLocal<ItemStackRenderState> TL_STATE =
            ThreadLocal.withInitial(ItemStackRenderState::new);

    private static final ThreadLocal<SubmitNodeStorage> TL_STORAGE =
            ThreadLocal.withInitial(SubmitNodeStorage::new);

    private static final ThreadLocal<RecordingVertexConsumer> TL_REC =
            ThreadLocal.withInitial(RecordingVertexConsumer::new);

    private static final ThreadLocal<ThreadGeoCache> TL_GEO_CACHE =
            ThreadLocal.withInitial(ThreadGeoCache::new);

    private static final ThreadLocal<ArrayList<CachedQuad>> TL_QUAD_LIST =
            ThreadLocal.withInitial(() -> new ArrayList<>(64));

    private static final ThreadLocal<PoseStack> TL_CAPTURE_POSE =
            ThreadLocal.withInitial(PoseStack::new);

    private static final ThreadLocal<PoseStack> TL_TESS_POSE =
            ThreadLocal.withInitial(PoseStack::new);

    /* ---------------- Tunables ---------------- */

    /**
     * If true: includes stack count in StackKey (correct more often, but worse cache hit rate).
     * If false: count is ignored (item count doesn't affect model geometry).
     */
    private static final boolean STRICT_STACK_KEY = false;

    /**
     * If true: discard glint render types during capture (glint is not cached).
     */
    private static final boolean SKIP_GLINT_GEOMETRY = true;

    /* ---------------- Debug / Atlas id ---------------- */

    private static final Logger LOG = LoggerFactory.getLogger("BBE/ShelfAtlasDebug");

    // Debug toggle
    private static final boolean DEBUG_ATLAS = false;

    private static volatile TextureAtlas BLOCK_ATLAS = null;
    private static volatile int BLOCK_ATLAS_ID = 0;
    private static volatile Identifier BLOCK_ATLAS_TEXTURE_ID = null;
    private static volatile TextureAtlasSprite MISSING = null;

    // sprite churn
    private static final ConcurrentHashMap<Identifier, Boolean> SEEN_SPRITES = new ConcurrentHashMap<>();
    private static final AtomicLong SPRITES_SEEN = new AtomicLong();
    private static final AtomicLong SPRITES_NOT_BLOCK_ATLAS = new AtomicLong();
    private static final AtomicLong DEBUG_EMIT_COUNT = new AtomicLong();

    // counters
    private static final AtomicLong MAP_ITEM_CALLS = new AtomicLong();
    private static final AtomicLong MAP_ITEM_HITS  = new AtomicLong();
    private static final AtomicLong MAP_ITEM_MISS  = new AtomicLong();

    private static final AtomicLong MAP_ENTITY_CALLS = new AtomicLong();
    private static final AtomicLong MAP_ENTITY_HITS  = new AtomicLong();
    private static final AtomicLong MAP_ENTITY_MISS  = new AtomicLong();

    private static final AtomicLong RENDER_TYPE_TEX_CALLS = new AtomicLong();
    private static final AtomicLong RENDER_TYPE_TEX_HITS  = new AtomicLong();
    private static final AtomicLong RENDER_TYPE_TEX_MISS  = new AtomicLong();

    /* ---------------- Sprite mapping cache ---------------- */

    private static final ConcurrentHashMap<TextureAtlasSprite, TextureAtlasSprite> ITEM_TO_BLOCK_CACHE = new ConcurrentHashMap<>();

    /* ---------------- RenderType caches (layer + glint + textureId) ---------------- */

    private record RenderTypeInfo(ChunkSectionLayer layer, boolean glint) {}

    private static final ConcurrentHashMap<Object, RenderTypeInfo> RENDER_TYPE_INFO_CACHE = new ConcurrentHashMap<>();

    // Sentinel for "no texture id"
    private static final Identifier TEX_MISS = Identifier.tryParse("bbe:__rt_tex_miss");

    private static final ConcurrentHashMap<RenderType, Identifier> RENDER_TYPE_TEX_CACHE = new ConcurrentHashMap<>();

    /* ---------------- Baked geometry cache ---------------- */

    private static final int NO_TINT = Integer.MIN_VALUE;

    /**
     * Per-thread LRU capacity. Kept smaller than the old global cap to avoid per-thread blowup.
     */
    private static final int GEO_CACHE_MAX_PER_THREAD = 1024;

    /**
     * Global epoch; bump to invalidate all per-thread caches.
     */
    private static final AtomicInteger GEO_CACHE_EPOCH = new AtomicInteger(0);

    private record StackKey(
            int itemRawId,
            int countOrZero,
            int componentsHash
    ) {}

    private record GeoKey(
            StackKey stack,
            int slot,
            int facing2d,
            boolean alignBottom
    ) {}

    private record CachedQuad(
            BakedQuad quad,
            ChunkSectionLayer layer,
            TextureAtlasSprite spriteForCacheOrNull,
            int tint
    ) {}

    private record CachedMesh(CachedQuad[] quads) {
        boolean isEmpty() { return quads.length == 0; }
    }

    private static final class LruMap<K, V> extends LinkedHashMap<K, V> {
        private final int max;
        LruMap(int max) {
            super(256, 0.75f, true);
            this.max = max;
        }
        @Override protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
            return size() > max;
        }
    }

    private static final class ThreadGeoCache {
        int epoch = GEO_CACHE_EPOCH.get();
        final LruMap<GeoKey, CachedMesh> map = new LruMap<>(GEO_CACHE_MAX_PER_THREAD);

        void refreshEpoch() {
            int e = GEO_CACHE_EPOCH.get();
            if (epoch != e) {
                epoch = e;
                map.clear();
            }
        }
    }

    /* ---------------- Atlas safety ---------------- */

    /**
     * If atlas is temporarily unavailable, return true and DON'T touch globals.
     */
    private static boolean ensureBlockAtlas() {
        final TextureAtlas atlas;
        try {
            atlas = Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS);
        } catch (Throwable t) {
            return true;
        }

        final int id = System.identityHashCode(atlas);

        if (BLOCK_ATLAS == atlas && BLOCK_ATLAS_ID == id && BLOCK_ATLAS_TEXTURE_ID != null && MISSING != null) {
            return false;
        }

        synchronized (ShelfItemEmitter.class) {
            // re-check under lock
            if (BLOCK_ATLAS == atlas && BLOCK_ATLAS_ID == id && BLOCK_ATLAS_TEXTURE_ID != null && MISSING != null) {
                return false;
            }

            // Atlas changed -> invalidate everything
            BLOCK_ATLAS = atlas;
            BLOCK_ATLAS_ID = id;
            BLOCK_ATLAS_TEXTURE_ID = atlas.location();

            try {
                MISSING = atlas.getSprite(Identifier.withDefaultNamespace("missingno"));
            } catch (Throwable t) {
                // If this happens, treat as "not ready"
                MISSING = null;
            }

            // Clear per-atlas caches/tracking
            ITEM_TO_BLOCK_CACHE.clear();
            SEEN_SPRITES.clear();

            RENDER_TYPE_INFO_CACHE.clear();
            RENDER_TYPE_TEX_CACHE.clear();

            SPRITES_SEEN.set(0);
            SPRITES_NOT_BLOCK_ATLAS.set(0);

            MAP_ITEM_CALLS.set(0); MAP_ITEM_HITS.set(0); MAP_ITEM_MISS.set(0);
            MAP_ENTITY_CALLS.set(0); MAP_ENTITY_HITS.set(0); MAP_ENTITY_MISS.set(0);
            RENDER_TYPE_TEX_CALLS.set(0); RENDER_TYPE_TEX_HITS.set(0); RENDER_TYPE_TEX_MISS.set(0);

            DEBUG_EMIT_COUNT.set(0);
        }

        return (BLOCK_ATLAS == null || BLOCK_ATLAS_TEXTURE_ID == null || MISSING == null);
    }

    private static boolean isNotBlockAtlasSprite(TextureAtlasSprite sprite) {
        if (sprite == null) return true;

        // ensure atlas id is valid; if unavailable, treat as "not in the block atlas"
        if (BLOCK_ATLAS_TEXTURE_ID == null && ensureBlockAtlas()) return true;

        Identifier loc = sprite.atlasLocation();
        return (BLOCK_ATLAS_TEXTURE_ID == null) || !BLOCK_ATLAS_TEXTURE_ID.equals(loc);
    }

    private static TextureAtlasSprite missingNo() {
        if (ensureBlockAtlas()) return null;
        return MISSING;
    }

    /* ---------------- Public entrypoints ---------------- */

    public static void emit(MutableQuadViewImpl emitter, BlockPos pos, BlockAndTintGetter levelView, ShelfBlockEntity shelf) {
        Level level = (levelView instanceof Level l) ? l : shelf.getLevel();
        if (level == null) return;
        emit(emitter, pos, level, shelf);
    }

    public static void emit(MutableQuadViewImpl emitter, BlockPos pos, Level level, ShelfBlockEntity shelf) {
        // during in-world reload: if atlas isn't ready, just skip
        if (ensureBlockAtlas()) return;

        final ThreadGeoCache cache = TL_GEO_CACHE.get();
        cache.refreshEpoch();

        final int epoch = GEO_CACHE_EPOCH.get();
        final ShelfSlotMeshCacheAccess slotCache = (ShelfSlotMeshCacheAccess) shelf;

        final int light = LevelRenderer.getLightColor(level, pos);

        // compute transform bits once per shelf
        Direction facing = shelf.getBlockState().getValue(ShelfBlock.FACING);
        final int facing2d = facing.get2DDataValue();
        final boolean alignBottom = shelf.getAlignItemsToBottom();

        for (int slot = 0; slot < 3; slot++) {
            ItemStack stack = shelf.getItems().get(slot);
            if (stack.isEmpty()) continue;

            // Allocation-free signature for per-shelf cache hit path
            int itemRawId = net.minecraft.core.registries.BuiltInRegistries.ITEM.getId(stack.getItem());
            int componentsHash = safeComponentsHash(stack);
            int countOrZero = STRICT_STACK_KEY ? stack.getCount() : 0;

            long sig0 = packSig0(itemRawId, componentsHash);
            long sig1 = packSig1(countOrZero, facing2d, alignBottom);

            Object cachedObj = slotCache.getSlotMesh(slot, epoch, sig0, sig1);
            if (cachedObj instanceof CachedMesh cachedMesh) {
                emitCached(emitter, cachedMesh);
                continue;
            }

            // Only allocate keys on miss (TL LRU + bake path)
            StackKey sk = new StackKey(itemRawId, countOrZero, componentsHash);
            GeoKey key = new GeoKey(sk, slot, facing2d, alignBottom);

            CachedMesh mesh = cache.map.get(key);
            if (mesh == null) {
                mesh = bakeMesh(level, shelf, slot, stack, light, sk);
                if (mesh == null || mesh.isEmpty()) continue;
                cache.map.put(key, mesh);
            }

            slotCache.putSlotMesh(slot, epoch, sig0, sig1, mesh);
            emitCached(emitter, mesh);
        }

        if (DEBUG_ATLAS) {
            long emits = DEBUG_EMIT_COUNT.incrementAndGet();
            if ((emits & 255) == 0) {
                LOG.warn("[SUMMARY] emits={} distinctSprites={} nonBlockSprites={} mapItem(calls/hits/miss)={}/{}/{} mapEntity(calls/hits/miss)={}/{}/{} rtTex(calls/hits/miss)={}/{}/{} blockAtlasTex={}",
                        emits,
                        SPRITES_SEEN.get(),
                        SPRITES_NOT_BLOCK_ATLAS.get(),
                        MAP_ITEM_CALLS.get(), MAP_ITEM_HITS.get(), MAP_ITEM_MISS.get(),
                        MAP_ENTITY_CALLS.get(), MAP_ENTITY_HITS.get(), MAP_ENTITY_MISS.get(),
                        RENDER_TYPE_TEX_CALLS.get(), RENDER_TYPE_TEX_HITS.get(), RENDER_TYPE_TEX_MISS.get(),
                        BLOCK_ATLAS_TEXTURE_ID);
            }
        }
    }

    private static CachedMesh bakeMesh(Level level, ShelfBlockEntity shelf, int slot, ItemStack stack, int light, StackKey sk) {
        if (ensureBlockAtlas()) return null;

        ArrayList<CachedQuad> out = TL_QUAD_LIST.get();
        out.clear();
        out.ensureCapacity(64);

        captureShelfItemGeometry(level, shelf, slot, stack, light, sk,
                (quad, renderTypeObj, tintLayers) -> {

                    RenderTypeInfo info = getRenderTypeInfo(renderTypeObj);
                    if (SKIP_GLINT_GEOMETRY && info.glint()) return;

                    ChunkSectionLayer layer = info.layer();

                    TextureAtlasSprite srcSprite = quad.sprite();

                    if (DEBUG_ATLAS && srcSprite != null) {
                        Identifier sid = srcSprite.contents().name();
                        if (SEEN_SPRITES.putIfAbsent(sid, Boolean.TRUE) == null) {
                            long n = SPRITES_SEEN.incrementAndGet();
                            if (isNotBlockAtlasSprite(srcSprite)) {
                                long nb = SPRITES_NOT_BLOCK_ATLAS.incrementAndGet();
                                LOG.warn("[SPRITE] new sprite seen #{} (non-block-atlas #{}): {} atlas={} blockAtlasTex={}",
                                        n, nb, sid, srcSprite.atlasLocation(), BLOCK_ATLAS_TEXTURE_ID);
                            } else if ((n % 256) == 0) {
                                LOG.warn("[SPRITE] distinct sprites seen so far: {}", n);
                            }
                        }
                    }

                    // If the quad's sprite is NOT on the block atlas, remap UVs once at bake time
                    // and store a baked quad already targeting the stitched (block atlas) sprite.
                    BakedQuad baked = quad;
                    TextureAtlasSprite spriteForCache = null;

                    if (isNotBlockAtlasSprite(srcSprite)) {
                        TextureAtlasSprite target = tryGetStitchedBlockItemSprite(srcSprite);
                        if (target == null) target = missingNo(); // avoid "wrong atlas UVs" rendering
                        if (target != null) {
                            baked = remapQuadToSprite(quad, target);
                            spriteForCache = target;
                        }
                    }

                    int tint = NO_TINT;
                    int ti = quad.tintIndex();
                    if (ti >= 0 && tintLayers != null && ti < tintLayers.length) {
                        tint = tintLayers[ti];
                    }

                    out.add(new CachedQuad(baked, layer, spriteForCache, tint));
                }
        );

        out.sort(Comparator.comparing(CachedQuad::layer));

        CachedQuad[] arr = out.toArray(CachedQuad[]::new);
        out.clear();
        return new CachedMesh(arr);
    }

    private static void emitCached(MutableQuadViewImpl emitter, CachedMesh mesh) {
        CachedQuad[] qs = mesh.quads();
        for (CachedQuad cq : qs) {
            emitter.fromBakedQuad(cq.quad());
            emitter.setCullFace(null);
            emitter.setRenderType(cq.layer());
            emitter.setQuadAtlas(SodiumQuadAtlas.BLOCK);

            TextureAtlasSprite s = cq.spriteForCacheOrNull();
            if (s != null) {
                emitter.cachedSprite(s);
            }

            if (cq.tint() != NO_TINT) {
                int tint = cq.tint();
                for (int v = 0; v < 4; v++) emitter.setColor(v, tint);
            }

            emitter.emitDirectly();
            emitter.clear();
        }
    }

    /* ---------------- Geometry capture ---------------- */

    @FunctionalInterface private interface Sink {
        void accept(BakedQuad quad, Object renderTypeObj, int[] tintLayers);
    }

    /**
     * Capture + tessellate item submit geometry.
     * NOTE about seeding:
     * We don't use pos-based seeds because that destroys cache reuse across shelves/chunks.
     * Instead, we use a stable seed derived from the stack render identity + slot.
     */
    private static void captureShelfItemGeometry(
            Level level,
            ShelfBlockEntity shelf,
            int slot,
            ItemStack stack,
            int light,
            StackKey sk,
            Sink sink
    ) {
        Minecraft mc = Minecraft.getInstance();
        ItemModelResolver resolver = mc.getItemModelResolver();

        ItemStackRenderState state = TL_STATE.get();
        state.clear();

        int seedBase = stableSeed(sk);

        resolver.updateForTopItem(
                state,
                stack,
                ItemDisplayContext.ON_SHELF,
                level,
                shelf,
                seedBase + slot
        );

        if (state.isEmpty()) return;

        PoseStack pose = TL_CAPTURE_POSE.get();
        resetPoseToIdentity(pose);

        Direction facing = shelf.getBlockState().getValue(ShelfBlock.FACING);
        float rotY = facing.getAxis().isHorizontal() ? -facing.toYRot() : 180.0F;

        float g = (float) (slot - 1) * 0.3125F;
        double offY = shelf.getAlignItemsToBottom() ? -0.25D : 0.0D;
        double offZ = -0.25D;

        pose.pushPose();
        pose.translate(0.5F, 0.5F, 0.5F);
        pose.mulPose(Axis.YP.rotationDegrees(rotY));
        pose.translate(g, offY, offZ);
        pose.scale(0.25F, 0.25F, 0.25F);

        AABB aabb = state.getModelBoundingBox();
        double d = -aabb.minY;
        if (!shelf.getAlignItemsToBottom()) {
            d += -(aabb.maxY - aabb.minY) / 2.0D;
        }
        pose.translate(0.0D, d, 0.0D);

        SubmitNodeStorage storage = TL_STORAGE.get();
        storage.clear();

        state.submit(pose, storage, light, OverlayTexture.NO_OVERLAY, 0);

        for (SubmitNodeCollection coll : storage.getSubmitsPerOrder().values()) {
            readItemSubmits(coll, sink);
            readModelSubmits(coll, sink);
            readModelPartSubmits(coll, sink);
            readCustomGeometrySubmits(coll, sink);
        }
        pose.popPose();
    }

    private static int stableSeed(StackKey sk) {
        int h = 0x9747b28c;
        h ^= sk.itemRawId();     h *= 0x85ebca6b;
        h ^= sk.componentsHash();h *= 0xc2b2ae35;
        h ^= sk.countOrZero();   h *= 0x27d4eb2d;

        // final mix
        h ^= (h >>> 16);
        h *= 0x85ebca6b;
        h ^= (h >>> 13);
        h *= 0xc2b2ae35;
        h ^= (h >>> 16);
        return h;
    }

    private static void readItemSubmits(SubmitNodeCollection coll, Sink sink) {
        for (SubmitNodeStorage.ItemSubmit submit : coll.getItemSubmits()) {
            Matrix4f mat = submit.pose().pose();
            int[] tints = submit.tintLayers();
            RenderType rt = submit.renderType();

            for (BakedQuad quad : submit.quads()) {
                sink.accept(transformQuadPositions(quad, mat), rt, tints);
            }
        }
    }

    private static void readModelSubmits(SubmitNodeCollection coll, Sink sink) {
        ModelFeatureRenderer.Storage storage = coll.getModelSubmits();

        var acc = (ModelFeatureRendererStorageAccessor) storage;
        var opaque = acc.getOpaqueModelSubmits();
        var translucent = acc.getTranslucentModelSubmits();

        for (var e : opaque.entrySet()) {
            RenderType rt = e.getKey();
            for (SubmitNodeStorage.ModelSubmit<?> sub : e.getValue()) {
                tessellateModelSubmit(rt, sub, sink);
            }
        }

        for (SubmitNodeStorage.TranslucentModelSubmit<?> t : translucent) {
            tessellateModelSubmit(t.renderType(), t.modelSubmit(), sink);
        }
    }

    private static <S> void tessellateModelSubmit(RenderType rt, SubmitNodeStorage.ModelSubmit<S> sub, Sink sink) {
        RecordingVertexConsumer rec = TL_REC.get();
        rec.clear();

        PoseStack ps = TL_TESS_POSE.get();
        resetPoseToIdentity(ps);
        ps.last().set(sub.pose());

        TextureAtlasSprite sprite = sub.sprite();

        if (sprite == null) {
            Identifier tex = tryExtractTextureId(rt);
            TextureAtlasSprite stitched = (tex != null) ? tryGetStitchedEntitySprite(tex) : null;
            rec.setActiveSprite(stitched);
        } else {
            rec.setActiveSprite(null);
        }

        VertexConsumer out = wrapSprite(sprite, rec);

        Model<? super S> model = sub.model();
        model.setupAnim(sub.state());
        model.renderToBuffer(ps, out, sub.lightCoords(), sub.overlayCoords(), sub.tintedColor());

        rec.flush();
        rec.remapUvsToActiveSpriteIfPresent();

        TextureAtlasSprite finalSprite = (sprite != null) ? sprite : rec.getActiveSprite();
        emitRecordedAsQuads(rt, rec.vertices(), finalSprite, sub.tintedColor(), sink);
    }

    private static void readModelPartSubmits(SubmitNodeCollection coll, Sink sink) {
        ModelPartFeatureRenderer.Storage storage = coll.getModelPartSubmits();

        var acc = (ModelPartFeatureRendererStorageAccessor) storage;
        var map = acc.getModelPartSubmits();

        for (var e : map.entrySet()) {
            RenderType rt = e.getKey();
            for (SubmitNodeStorage.ModelPartSubmit sub : e.getValue()) {
                tessellateModelPartSubmit(rt, sub, sink);
            }
        }
    }

    private static void tessellateModelPartSubmit(RenderType rt, SubmitNodeStorage.ModelPartSubmit sub, Sink sink) {
        RecordingVertexConsumer rec = TL_REC.get();
        rec.clear();

        PoseStack ps = TL_TESS_POSE.get();
        resetPoseToIdentity(ps);
        ps.last().set(sub.pose());

        VertexConsumer out = wrapSprite(sub.sprite(), rec);

        ModelPart part = sub.modelPart();
        part.render(ps, out, sub.lightCoords(), sub.overlayCoords(), sub.tintedColor());

        rec.flush();

        emitRecordedAsQuads(rt, rec.vertices(), sub.sprite(), sub.tintedColor(), sink);
    }

    private static void readCustomGeometrySubmits(SubmitNodeCollection coll, Sink sink) {
        CustomFeatureRenderer.Storage storage = coll.getCustomGeometrySubmits();

        var acc = (CustomFeatureRendererStorageAccessor) storage;
        var map = acc.getCustomGeometrySubmits();

        for (var e : map.entrySet()) {
            RenderType rt = e.getKey();
            for (SubmitNodeStorage.CustomGeometrySubmit sub : e.getValue()) {
                tessellateCustomSubmit(rt, sub, sink);
            }
        }
    }

    private static void tessellateCustomSubmit(RenderType rt, SubmitNodeStorage.CustomGeometrySubmit sub, Sink sink) {
        RecordingVertexConsumer rec = TL_REC.get();
        rec.clear();

        sub.customGeometryRenderer().render(sub.pose(), rec);

        rec.flush();

        emitRecordedAsTris(rt, rec.vertices(), sink);
    }

    /* ---------------- Recorded emission ---------------- */

    private static void emitRecordedAsQuads(
            RenderType rt,
            List<RecordingVertexConsumer.Vtx> verts,
            TextureAtlasSprite spriteOrNull,
            int tintedColor,
            Sink sink
    ) {
        if (verts.isEmpty()) return;

        int[] tintLayers = new int[]{ tintedColor };
        int tintIndex = 0;

        // if we can't get a safe missing sprite during reload, just skip emission
        TextureAtlasSprite sprite = (spriteOrNull != null) ? spriteOrNull : missingNo();
        if (sprite == null) return;

        int n = verts.size();
        if ((n & 3) != 0) {
            emitRecordedAsTris(rt, verts, sink);
            return;
        }

        for (int i = 0; i + 3 < n; i += 4) {
            var a = verts.get(i);
            var b = verts.get(i + 1);
            var c = verts.get(i + 2);
            var d = verts.get(i + 3);

            Direction dir = guessDirection(a, b, c);
            BakedQuad q = makeBakedQuad(a, b, c, d, tintIndex, dir, sprite);
            sink.accept(q, rt, tintLayers);
        }
    }

    private static void emitRecordedAsTris(RenderType rt, List<RecordingVertexConsumer.Vtx> verts, Sink sink) {
        if (verts.isEmpty()) return;

        TextureAtlasSprite sprite = missingNo();
        if (sprite == null) return;

        int n = verts.size();
        for (int i = 0; i + 2 < n; i += 3) {
            var a = verts.get(i);
            var b = verts.get(i + 1);
            var c = verts.get(i + 2);

            Direction dir = guessDirection(a, b, c);
            BakedQuad q = makeBakedQuad(a, b, c, c, -1, dir, sprite);
            sink.accept(q, rt, null);
        }
    }

    private static BakedQuad makeBakedQuad(
            RecordingVertexConsumer.Vtx a,
            RecordingVertexConsumer.Vtx b,
            RecordingVertexConsumer.Vtx c,
            RecordingVertexConsumer.Vtx d,
            int tintIndex,
            Direction dir,
            TextureAtlasSprite sprite
    ) {
        Vector3f p0 = new Vector3f(a.x, a.y, a.z);
        Vector3f p1 = new Vector3f(b.x, b.y, b.z);
        Vector3f p2 = new Vector3f(c.x, c.y, c.z);
        Vector3f p3 = new Vector3f(d.x, d.y, d.z);

        long uv0 = UVPair.pack(a.u, a.v);
        long uv1 = UVPair.pack(b.u, b.v);
        long uv2 = UVPair.pack(c.u, c.v);
        long uv3 = UVPair.pack(d.u, d.v);

        return new BakedQuad(
                p0, p1, p2, p3,
                uv0, uv1, uv2, uv3,
                tintIndex,
                dir,
                sprite,
                true,
                0
        );
    }

    private static Direction guessDirection(RecordingVertexConsumer.Vtx a, RecordingVertexConsumer.Vtx b, RecordingVertexConsumer.Vtx c) {
        float ax = b.x - a.x, ay = b.y - a.y, az = b.z - a.z;
        float bx = c.x - a.x, by = c.y - a.y, bz = c.z - a.z;

        float nx = ay * bz - az * by;
        float ny = az * bx - ax * bz;
        float nz = ax * by - ay * bx;

        float anx = Math.abs(nx), any = Math.abs(ny), anz = Math.abs(nz);
        if (anx >= any && anx >= anz) return (nx >= 0) ? Direction.EAST : Direction.WEST;
        if (any >= anx && any >= anz) return (ny >= 0) ? Direction.UP : Direction.DOWN;

        return (nz >= 0) ? Direction.SOUTH : Direction.NORTH;
    }

    /* ---------------- Helpers ---------------- */

    private static VertexConsumer wrapSprite(TextureAtlasSprite sprite, VertexConsumer base) {
        if (sprite == null) return base;
        return sprite.wrap(base);
    }

    private static BakedQuad transformQuadPositions(BakedQuad q, Matrix4f m) {
        Vector3f p0 = new Vector3f(q.position0());
        Vector3f p1 = new Vector3f(q.position1());
        Vector3f p2 = new Vector3f(q.position2());
        Vector3f p3 = new Vector3f(q.position3());

        m.transformPosition(p0);
        m.transformPosition(p1);
        m.transformPosition(p2);
        m.transformPosition(p3);

        return new BakedQuad(
                p0, p1, p2, p3,
                q.packedUV0(), q.packedUV1(), q.packedUV2(), q.packedUV3(),
                q.tintIndex(),
                q.direction(),
                q.sprite(),
                q.shade(),
                q.lightEmission()
        );
    }

    private static RenderTypeInfo getRenderTypeInfo(Object renderTypeObj) {
        if (renderTypeObj == null) return new RenderTypeInfo(ChunkSectionLayer.CUTOUT, false);

        RenderTypeInfo cached = RENDER_TYPE_INFO_CACHE.get(renderTypeObj);
        if (cached != null) return cached;

        String s;
        if (renderTypeObj instanceof RenderType rt) s = rt.toString();
        else s = renderTypeObj.toString();

        String n = s.toLowerCase(Locale.ROOT);

        ChunkSectionLayer layer;
        if (n.contains("translucent")) layer = ChunkSectionLayer.TRANSLUCENT;
        else if (n.contains("solid")) layer = ChunkSectionLayer.SOLID;
        else layer = ChunkSectionLayer.CUTOUT;

        boolean glint = n.contains("glint") || n.contains("foil");

        RenderTypeInfo info = new RenderTypeInfo(layer, glint);
        RENDER_TYPE_INFO_CACHE.put(renderTypeObj, info);
        return info;
    }

    /**
     * Remap the quad UVs from its current sprite to the target sprite.
     * This is the bake-time replacement for per-hit QuadTransform.swapSprite().
     */
    private static BakedQuad remapQuadToSprite(BakedQuad q, TextureAtlasSprite target) {
        TextureAtlasSprite src = q.sprite();
        if (target == null || src == target) return q;

        float su0 = src.getU0(), su1 = src.getU1();
        float sv0 = src.getV0(), sv1 = src.getV1();

        float tu0 = target.getU0(), tu1 = target.getU1();
        float tv0 = target.getV0(), tv1 = target.getV1();

        float sdu = su1 - su0;
        float sdv = sv1 - sv0;

        if (sdu == 0.0f || sdv == 0.0f) {
            return new BakedQuad(
                    q.position0(), q.position1(), q.position2(), q.position3(),
                    q.packedUV0(), q.packedUV1(), q.packedUV2(), q.packedUV3(),
                    q.tintIndex(),
                    q.direction(),
                    target,
                    q.shade(),
                    q.lightEmission()
            );
        }

        long uv0 = remapPackedUV(q.packedUV0(), su0, sdu, sv0, sdv, tu0, tu1, tv0, tv1);
        long uv1 = remapPackedUV(q.packedUV1(), su0, sdu, sv0, sdv, tu0, tu1, tv0, tv1);
        long uv2 = remapPackedUV(q.packedUV2(), su0, sdu, sv0, sdv, tu0, tu1, tv0, tv1);
        long uv3 = remapPackedUV(q.packedUV3(), su0, sdu, sv0, sdv, tu0, tu1, tv0, tv1);

        return new BakedQuad(
                q.position0(), q.position1(), q.position2(), q.position3(),
                uv0, uv1, uv2, uv3,
                q.tintIndex(),
                q.direction(),
                target,
                q.shade(),
                q.lightEmission()
        );
    }

    private static long remapPackedUV(
            long packed,
            float su0, float sdu,
            float sv0, float sdv,
            float tu0, float tu1,
            float tv0, float tv1
    ) {
        float u = unpackU(packed);
        float v = unpackV(packed);

        float fu = (u - su0) / sdu;
        float fv = (v - sv0) / sdv;

        float nu = tu0 + fu * (tu1 - tu0);
        float nv = tv0 + fv * (tv1 - tv0);

        return packUV(nu, nv);
    }

    // Packed UV format: high 32 bits = float u bits, low 32 bits = float v bits (matches UVPair.pack usage).
    private static float unpackU(long packed) {
        return Float.intBitsToFloat((int) (packed >>> 32));
    }

    private static float unpackV(long packed) {
        return Float.intBitsToFloat((int) (packed));
    }

    private static long packUV(float u, float v) {
        return ((long) Float.floatToRawIntBits(u) << 32) | (Float.floatToRawIntBits(v) & 0xffffffffL);
    }

    private static void resetPoseToIdentity(PoseStack ps) {
        ps.last().pose().identity();
        ps.last().normal().identity();
    }

    /* ---------------- Item sprite -> stitched block sprite mapping ---------------- */

    private static TextureAtlasSprite tryGetStitchedBlockItemSprite(TextureAtlasSprite srcSprite) {
        if (DEBUG_ATLAS) MAP_ITEM_CALLS.incrementAndGet();

        if (srcSprite == null) {
            if (DEBUG_ATLAS) MAP_ITEM_MISS.incrementAndGet();
            return null;
        }

        if (BLOCK_ATLAS == null || MISSING == null) {
            if (ensureBlockAtlas()) {
                if (DEBUG_ATLAS) MAP_ITEM_MISS.incrementAndGet();
                return null;
            }
            if (BLOCK_ATLAS == null || MISSING == null) {
                if (DEBUG_ATLAS) MAP_ITEM_MISS.incrementAndGet();
                return null;
            }
        }

        // Fast path: identity-keyed lookup
        TextureAtlasSprite cached = ITEM_TO_BLOCK_CACHE.get(srcSprite);
        if (cached != null) {
            if (cached == MISSING) {
                if (DEBUG_ATLAS) MAP_ITEM_MISS.incrementAndGet();
                return null;
            }
            if (DEBUG_ATLAS) MAP_ITEM_HITS.incrementAndGet();
            return cached;
        }

        // Slow path: derive candidates from the sprite's name only on miss
        Identifier srcId = srcSprite.contents().name();

        TextureAtlasSprite result = null;
        for (Identifier cand : blockAtlasItemCandidates(srcId)) {
            TextureAtlasSprite s = BLOCK_ATLAS.getSprite(cand);
            if (s != MISSING && s.contents() != MISSING.contents()) {
                result = s;
                break;
            }
        }

        ITEM_TO_BLOCK_CACHE.put(srcSprite, (result != null) ? result : MISSING);

        if (DEBUG_ATLAS) {
            if (result != null) MAP_ITEM_HITS.incrementAndGet();
            else MAP_ITEM_MISS.incrementAndGet();
        }

        return result;
    }

    private static List<Identifier> blockAtlasItemCandidates(Identifier srcId) {
        String ns = srcId.getNamespace();
        String path = srcId.getPath();

        ArrayList<Identifier> out = new ArrayList<>(4);

        out.add(srcId);

        String name;
        if (path.startsWith("item/")) {
            name = path.substring("item/".length());
        } else if (path.contains("item/")) {
            name = path.substring(path.indexOf("item/") + "item/".length());
        } else {
            name = path;
        }

        addIfParse(out, ns, "item/" + name);
        addIfParse(out, "minecraft", "item/" + ns + "/" + name);

        return out;
    }

    private static void addIfParse(List<Identifier> out, String ns, String path) {
        Identifier id = Identifier.tryParse(ns + ":" + path);
        if (id != null) out.add(id);
    }

    private static int safeComponentsHash(ItemStack stack) {
        try {
            return stack.getComponents().hashCode();
        } catch (Throwable t) {
            return 0;
        }
    }

    private static long packSig0(int itemRawId, int componentsHash) {
        return ((long) itemRawId << 32) | (componentsHash & 0xffffffffL);
    }

    private static long packSig1(int countOrZero, int facing2d, boolean alignBottom) {
        long flags = (facing2d & 0x3L) | ((alignBottom ? 1L : 0L) << 2);
        return ((long) countOrZero << 32) | flags;
    }

    /* ---------------- Entity texture -> stitched sprite mapping ---------------- */

    @SuppressWarnings("ConstantConditions") private static Identifier tryExtractTextureId(RenderType rt) {
        if (DEBUG_ATLAS) RENDER_TYPE_TEX_CALLS.incrementAndGet();
        if (rt == null) return null;

        Identifier cached = RENDER_TYPE_TEX_CACHE.get(rt);
        if (cached != null) {
            if (DEBUG_ATLAS) RENDER_TYPE_TEX_HITS.incrementAndGet();
            return (cached == TEX_MISS) ? null : cached;
        }

        try {
            Object setup = ((RenderTypeAccessor) rt).GetState();
            if (!(setup instanceof RenderSetupAccessor rsa)) {
                RENDER_TYPE_TEX_CACHE.put(rt, TEX_MISS);
                return null;
            }

            Map<String, Object> textures = rsa.GetTexture();
            if (textures == null || textures.isEmpty()) {
                RENDER_TYPE_TEX_CACHE.put(rt, TEX_MISS);
                return null;
            }

            Object binding = textures.values().iterator().next();
            if (!(binding instanceof TextureBindingAccessor tba)) {
                RENDER_TYPE_TEX_CACHE.put(rt, TEX_MISS);
                return null;
            }

            Identifier out = tba.GetLocation();
            RENDER_TYPE_TEX_CACHE.put(rt, out != null ? out : TEX_MISS);

            if (DEBUG_ATLAS) RENDER_TYPE_TEX_HITS.incrementAndGet();
            return out;
        } catch (Throwable t) {
            RENDER_TYPE_TEX_CACHE.put(rt, TEX_MISS);

            if (DEBUG_ATLAS) {
                long m = RENDER_TYPE_TEX_MISS.incrementAndGet();
                if ((m % 64) == 0) {
                    LOG.warn("[RT_TEX] misses={} calls={} hits={}", m, RENDER_TYPE_TEX_CALLS.get(), RENDER_TYPE_TEX_HITS.get());
                }
            }
            return null;
        }
    }

    private static TextureAtlasSprite tryGetStitchedEntitySprite(Identifier textureId) {
        if (DEBUG_ATLAS) MAP_ENTITY_CALLS.incrementAndGet();

        if (textureId == null) {
            if (DEBUG_ATLAS) MAP_ENTITY_MISS.incrementAndGet();
            return null;
        }

        if (BLOCK_ATLAS == null || MISSING == null) {
            if (ensureBlockAtlas()) {
                if (DEBUG_ATLAS) MAP_ENTITY_MISS.incrementAndGet();
                return null;
            }
            if (BLOCK_ATLAS == null || MISSING == null) {
                if (DEBUG_ATLAS) MAP_ENTITY_MISS.incrementAndGet();
                return null;
            }
        }

        String ns = textureId.getNamespace();
        String path = textureId.getPath();

        Identifier spriteId;

        if (path.startsWith("textures/") && path.endsWith(".png")) {
            String core = path.substring("textures/".length(), path.length() - ".png".length());
            spriteId = Identifier.tryParse(ns + ":" + core);
        }

        else if (path.endsWith(".png")) {
            String core = path.substring(0, path.length() - ".png".length());
            spriteId = Identifier.tryParse(ns + ":" + core);
        }

        else {
            spriteId = textureId;
        }

        if (spriteId == null) {
            if (DEBUG_ATLAS) MAP_ENTITY_MISS.incrementAndGet();
            return null;
        }

        TextureAtlasSprite s = BLOCK_ATLAS.getSprite(spriteId);
        if (s == MISSING || s.contents() == MISSING.contents()) {
            if (DEBUG_ATLAS) MAP_ENTITY_MISS.incrementAndGet();
            return null;
        }

        if (DEBUG_ATLAS) MAP_ENTITY_HITS.incrementAndGet();
        return s;
    }

    /* ---------------- Reload invalidation ---------------- */

    public static void invalidateAllCachesOnReload() {
        synchronized (ShelfItemEmitter.class) {
            // Force re-fetch of atlas next reload
            BLOCK_ATLAS = null;
            BLOCK_ATLAS_ID = 0;
            BLOCK_ATLAS_TEXTURE_ID = null;
            MISSING = null;

            // Clear all mappings + debug tracking
            ITEM_TO_BLOCK_CACHE.clear();
            SEEN_SPRITES.clear();

            RENDER_TYPE_INFO_CACHE.clear();
            RENDER_TYPE_TEX_CACHE.clear();

            // Invalidate all per-thread LRU caches
            GEO_CACHE_EPOCH.incrementAndGet();

            SPRITES_SEEN.set(0);
            SPRITES_NOT_BLOCK_ATLAS.set(0);
            DEBUG_EMIT_COUNT.set(0);

            MAP_ITEM_CALLS.set(0); MAP_ITEM_HITS.set(0); MAP_ITEM_MISS.set(0);
            MAP_ENTITY_CALLS.set(0); MAP_ENTITY_HITS.set(0); MAP_ENTITY_MISS.set(0);
            RENDER_TYPE_TEX_CALLS.set(0); RENDER_TYPE_TEX_HITS.set(0); RENDER_TYPE_TEX_MISS.set(0);
        }
    }
}
