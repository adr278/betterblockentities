package betterblockentities.client.chunk.pipeline;

/* local */
import betterblockentities.client.render.immediate.blockentity.ShelfSlotMeshCacheAccess;
import betterblockentities.client.chunk.pipeline.shelf.CacheKeys;
import betterblockentities.client.chunk.pipeline.shelf.GeometryBaker;
import betterblockentities.client.chunk.pipeline.shelf.LongMeshCache;
import betterblockentities.client.chunk.pipeline.shelf.RenderTypeClassifier;
import betterblockentities.client.chunk.pipeline.shelf.SpriteRemapper;

/* minecraft */
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.ShelfBlock;
import net.minecraft.world.level.block.entity.ShelfBlockEntity;

/* sodium */
import net.caffeinemc.mods.sodium.client.render.model.MutableQuadViewImpl;
import net.caffeinemc.mods.sodium.client.render.model.SodiumQuadAtlas;

/* java/misc */
import java.util.concurrent.atomic.AtomicInteger;

public final class ShelfItemEmitter {
    private ShelfItemEmitter() {}

    /* ---------------- Dependencies ---------------- */

    private static final RenderTypeClassifier RT =
            new RenderTypeClassifier();

    private static final SpriteRemapper SPRITES =
            new SpriteRemapper();

    private static final GeometryBaker BAKER =
            new GeometryBaker(RT, SPRITES);

    /* ---------------- Tunables ---------------- */

    /**
     * If true: includes stack count in StackKey.
     * If false: count is ignored (item count doesn't affect model geometry anyway).
     */
    private static final boolean STRICT_STACK_KEY = false;

    /**
     * If true: discard glint render types during capture (glint is not cached).
     */
    private static final boolean SKIP_GLINT_GEOMETRY = true;

    /* ---------------- Per-thread staging ---------------- */

    private static final ThreadLocal<ThreadGeoCache> TL_GEO_CACHE =
            ThreadLocal.withInitial(ThreadGeoCache::new);

    /* ---------------- Baked geometry cache ---------------- */

    /**
     * Per-thread cache capacity. (LongMeshCache wants a power-of-two.)
     */
    private static final int GEO_CACHE_CAPACITY_PER_THREAD = 1024;

    /**
     * Global epoch; bump to invalidate all per-thread caches.
     */
    private static final AtomicInteger GEO_CACHE_EPOCH =
            new AtomicInteger(0);

    private static final class ThreadGeoCache {
        int epoch = GEO_CACHE_EPOCH.get();

        final LongMeshCache<GeometryBaker.CachedMesh> map =
                new LongMeshCache<>(GEO_CACHE_CAPACITY_PER_THREAD);

        void refreshEpoch() {
            int e = GEO_CACHE_EPOCH.get();
            if (epoch != e) {
                epoch = e;
                map.clear();
            }
        }
    }

    /* ---------------- Public entrypoints ---------------- */

    public static void emit(MutableQuadViewImpl emitter, BlockPos pos, BlockAndTintGetter levelView, ShelfBlockEntity shelf) {
        Level level = (levelView instanceof Level l) ? l : shelf.getLevel();
        if (level == null) return;
        emit(emitter, pos, level, shelf);
    }

    public static void emit(MutableQuadViewImpl emitter, BlockPos pos, Level level, ShelfBlockEntity shelf) {
        final ThreadGeoCache cache = TL_GEO_CACHE.get();
        cache.refreshEpoch();

        final int epoch = GEO_CACHE_EPOCH.get();
        final ShelfSlotMeshCacheAccess slotCache = (ShelfSlotMeshCacheAccess) shelf;

        final int light = LevelRenderer.getLightColor(level, pos);

        // compute transform bits once per shelf.
        Direction facing = shelf.getBlockState().getValue(ShelfBlock.FACING);
        final int facing2d = CacheKeys.facing2d(facing);
        final boolean alignBottom = shelf.getAlignItemsToBottom();

        for (int slot = 0; slot < 3; slot++) {
            ItemStack stack = shelf.getItems().get(slot);
            if (stack.isEmpty()) continue;

            final int itemRawId = BuiltInRegistries.ITEM.getId(stack.getItem());
            final int componentsHash = safeComponentsHash(stack);
            final int countOrZero = STRICT_STACK_KEY ? stack.getCount() : 0;

            final long sig0 = CacheKeys.packSig0(itemRawId, componentsHash);
            final long sig1 = CacheKeys.packSig1WithSlot(countOrZero, facing2d, alignBottom, slot);

            // L1: per-shelf slot cache.
            Object cachedObj = slotCache.getSlotMesh(slot, epoch, sig0, sig1);
            if (cachedObj instanceof GeometryBaker.CachedMesh cachedMesh) {
                emitCached(emitter, cachedMesh, light);
                continue;
            }

            // L2: per-thread primitive cache.
            final long key = CacheKeys.mix64(sig0, sig1);

            GeometryBaker.CachedMesh mesh = cache.map.get(key);
            if (mesh == null) {
                CacheKeys.StackKey sk = new CacheKeys.StackKey(itemRawId, countOrZero, componentsHash);

                mesh = bakeMesh(level, shelf, slot, stack, light, sk);
                if (mesh.isEmpty()) continue;

                cache.map.put(key, mesh);
            }

            slotCache.putSlotMesh(slot, epoch, sig0, sig1, mesh);
            emitCached(emitter, mesh, light);
        }
    }

    private static GeometryBaker.CachedMesh bakeMesh(Level level, ShelfBlockEntity shelf, int slot, ItemStack stack, int light, CacheKeys.StackKey sk) {
        return BAKER.bakeMesh(level, shelf, slot, stack, light, sk, SKIP_GLINT_GEOMETRY);
    }

    private static void emitCached(MutableQuadViewImpl emitter, GeometryBaker.CachedMesh mesh, int light) {
        for (GeometryBaker.CachedQuad cq : mesh.quads()) {
            GeometryBaker.PackedQuad q = cq.quad();

            // Positions.
            emitter.setPos(0, q.x0(), q.y0(), q.z0());
            emitter.setPos(1, q.x1(), q.y1(), q.z1());
            emitter.setPos(2, q.x2(), q.y2(), q.z2());
            emitter.setPos(3, q.x3(), q.y3(), q.z3());

            // UVs (unpack from long).
            emitter.setUV(0, UVPair.unpackU(q.uv0()), UVPair.unpackV(q.uv0()));
            emitter.setUV(1, UVPair.unpackU(q.uv1()), UVPair.unpackV(q.uv1()));
            emitter.setUV(2, UVPair.unpackU(q.uv2()), UVPair.unpackV(q.uv2()));
            emitter.setUV(3, UVPair.unpackU(q.uv3()), UVPair.unpackV(q.uv3()));

            // Lighting: apply per-shelf at emit time (so cache is non-light dependent).
            emitter.setLight(0, light);
            emitter.setLight(1, light);
            emitter.setLight(2, light);
            emitter.setLight(3, light);

            // Metadata.
            emitter.setCullFace(null);
            emitter.setNominalFace(q.dir());
            emitter.setDiffuseShade(q.shade());
            emitter.setEmissive(q.lightEmission() == 15);
            emitter.setRenderType(cq.layer());
            emitter.setQuadAtlas(SodiumQuadAtlas.BLOCK);

            // Sprite: must match UV space.
            TextureAtlasSprite s = q.spriteForCacheOrNull();
            if (s == null) s = q.sprite();
            emitter.cachedSprite(s);

            // Tint: we solved tint color during bake.
            if (cq.tint() != CacheKeys.NO_TINT) {
                int tint = cq.tint();
                emitter.setColor(0, tint);
                emitter.setColor(1, tint);
                emitter.setColor(2, tint);
                emitter.setColor(3, tint);
            }

            emitter.emitDirectly();
            emitter.clear();
        }
    }

    private static int safeComponentsHash(ItemStack stack) {
        try {
            var comps = stack.getComponents();
            return comps.hashCode();
        } catch (Throwable t) {
            return 0;
        }
    }

    /* ---------------- Reload invalidation ---------------- */

    public static void invalidateAllCachesOnReload() {
        synchronized (ShelfItemEmitter.class) {
            // Clear helper caches
            SPRITES.clear();
            RT.clear();
            // Invalidate all per-thread caches.
            GEO_CACHE_EPOCH.incrementAndGet();
        }
    }
}
