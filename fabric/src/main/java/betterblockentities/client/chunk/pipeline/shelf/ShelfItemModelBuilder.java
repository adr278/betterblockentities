package betterblockentities.client.chunk.pipeline.shelf;

/* minecraft */
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.ShelfBlockEntity;

/* java */
import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.copyOf;

public final class ShelfItemModelBuilder {
    private ShelfItemModelBuilder() {}

    private static final RenderTypeClassifier RT = new RenderTypeClassifier();
    private static final SpriteRemapper SPRITES = new SpriteRemapper();
    private static final GeometryBaker BAKER = new GeometryBaker(RT, SPRITES);

    private static final boolean STRICT_STACK_KEY = false;
    private static final boolean SKIP_GLINT_GEOMETRY = true;

    private static final int GLOBAL_CACHE_CAPACITY = 4096;
    private static final int GLOBAL_CACHE_SHARDS = 16;
    private static final int SHARD_CAPACITY = Math.max(1, GLOBAL_CACHE_CAPACITY / GLOBAL_CACHE_SHARDS);

    private static final int SLOT_COUNT = 3;
    private static final int FACING_COUNT = 4;
    private static final int ALIGN_COUNT = 2;
    private static final int VARIANT_COUNT = SLOT_COUNT * FACING_COUNT * ALIGN_COUNT;

    private static final GeometryBaker.LayeredPart[] NO_PARTS = new GeometryBaker.LayeredPart[0];
    private static final GeometryBaker.CanonicalBounds ZERO_BOUNDS =
            new GeometryBaker.CanonicalBounds(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);

    @FunctionalInterface public interface PlacedPartConsumer {
        void accept(BlockStateModelPart transformedPart, GeometryBaker.LayeredPart layeredPart);
    }

    private static final class CachedMesh {
        private final long sig0;
        private final long sig1;
        private final GeometryBaker.LayeredPart[] parts;
        private final GeometryBaker.CanonicalBounds bounds;
        private final BlockStateModelPart[][] variants = new BlockStateModelPart[VARIANT_COUNT][];

        private CachedMesh(
                long sig0,
                long sig1,
                GeometryBaker.LayeredPart[] parts,
                GeometryBaker.CanonicalBounds bounds
        ) {
            this.sig0 = sig0;
            this.sig1 = sig1;
            this.parts = parts;
            this.bounds = bounds;
        }

        private static CachedMesh empty(long sig0, long sig1) {
            return new CachedMesh(sig0, sig1, NO_PARTS, ZERO_BOUNDS);
        }

        private static CachedMesh of(long sig0, long sig1, GeometryBaker.CanonicalMesh mesh) {
            return new CachedMesh(
                    sig0,
                    sig1,
                    copyOf(mesh.parts(), mesh.parts().length),
                    mesh.bounds()
            );
        }

        private long sig0() {
            return sig0;
        }

        private long sig1() {
            return sig1;
        }

        private GeometryBaker.LayeredPart[] parts() {
            return parts;
        }

        private BlockStateModelPart[] getVariantParts(int slot, Direction facing, boolean alignBottom) {
            int index = variantIndex(slot, facing, alignBottom);

            BlockStateModelPart[] cached = variants[index];
            if (cached != null) return cached;

            synchronized (variants) {
                cached = variants[index];
                if (cached == null) {
                    cached = ShelfPartTransformer.transformParts
                            (parts, bounds, slot, facing, alignBottom);
                    variants[index] = cached;
                }
                return cached;
            }
        }
    }

    private static final List<LongMeshCache<CachedMesh>> GLOBAL_PART_CACHE = createShards();

    private static List<LongMeshCache<CachedMesh>> createShards() {
        List<LongMeshCache<CachedMesh>> shards = new ArrayList<>(GLOBAL_CACHE_SHARDS);
        for (int i = 0; i < GLOBAL_CACHE_SHARDS; i++) {
            shards.add(new LongMeshCache<>(SHARD_CAPACITY, CachedMesh.class));
        }
        return shards;
    }

    public static void forEachPlacedPart(
            BlockAndTintGetter levelView,
            ShelfBlockEntity shelf,
            Direction facing,
            boolean alignBottom,
            PlacedPartConsumer consumer
    ) {
        Level level = levelView instanceof Level l ? l : shelf.getLevel();
        if (level != null) forEachPlacedPart(level, shelf, facing, alignBottom, consumer);
    }

    public static void forEachPlacedPart(
            Level level,
            ShelfBlockEntity shelf,
            Direction facing,
            boolean alignBottom,
            PlacedPartConsumer consumer
    ) {
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            ItemStack stack = shelf.getItems().get(slot);
            if (stack.isEmpty() || skipTerrainMeshingForStack(shelf, stack, slot)) continue;

            CachedMesh cached = getOrBakeCachedMesh(level, stack);
            if (cached == null) continue;

            GeometryBaker.LayeredPart[] layeredParts = cached.parts();
            BlockStateModelPart[] transformedParts = cached.getVariantParts(slot, facing, alignBottom);

            for (int i = 0; i < layeredParts.length; i++) {
                consumer.accept(transformedParts[i], layeredParts[i]);
            }
        }
    }

    private static boolean skipTerrainMeshingForStack(
            ShelfBlockEntity shelf,
            ItemStack stack,
            int slot
    ) {
        /* Special items do not need the immediate glint fallback */
        return ShelfItemImmediateFallback.shouldUseImmediateFallback(shelf, stack, slot);
    }

    private static CachedMesh getOrBakeCachedMesh(Level level, ItemStack stack) {
        int itemRawId = BuiltInRegistries.ITEM.getId(stack.getItem());
        int countOrZero = STRICT_STACK_KEY ? stack.getCount() : 0;

        Integer componentsHash = tryComponentsHash(stack);

        if (componentsHash == null)
            return bake(level, stack, itemRawId, countOrZero,
                    System.identityHashCode(stack), 0L, 0L, false);
        long sig0 = CacheKeys.packSig0(itemRawId, componentsHash);
        long sig1 = CacheKeys.packSig1(countOrZero);
        long key = CacheKeys.mix64(sig0, sig1);

        int shardIndex = shardIndex(key);
        LongMeshCache<CachedMesh> shard = GLOBAL_PART_CACHE.get(shardIndex);

        synchronized (shard) {
            CachedMesh cached = shard.get(key);
            if (cached != null && cached.sig0() == sig0 && cached.sig1() == sig1) {
                return cached.parts().length == 0 ? null : cached;
            }
        }

        CachedMesh baked = bake(level, stack, itemRawId, countOrZero, componentsHash, sig0, sig1, true);
        if (baked == null) return null;

        synchronized (shard) {
            CachedMesh cached = shard.get(key);
            if (cached != null && cached.sig0() == sig0 && cached.sig1() == sig1) {
                return cached.parts().length == 0 ? null : cached;
            }

            shard.put(key, baked);
            return baked.parts().length == 0 ? null : baked;
        }
    }

    private static CachedMesh bake(
            Level level,
            ItemStack stack,
            int itemRawId,
            int countOrZero,
            int hash,
            long sig0,
            long sig1,
            boolean cacheable
    ) {
        CacheKeys.StackKey key = new CacheKeys.StackKey(itemRawId, countOrZero, hash);
        GeometryBaker.CanonicalMesh mesh = BAKER.bakeCanonicalMesh(level, stack, key, SKIP_GLINT_GEOMETRY);

        if (mesh.parts().length == 0) return cacheable ? CachedMesh.empty(sig0, sig1) : null;

        return CachedMesh.of(sig0, sig1, mesh);
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

    private static int variantIndex(int slot, Direction facing, boolean alignBottom) {
        int facingIndex = switch (facing) {
            case EAST -> 1;
            case SOUTH -> 2;
            case WEST -> 3;
            default -> 0;
        };

        return ((slot * FACING_COUNT + facingIndex) << 1) | (alignBottom ? 1 : 0);
    }

    public static void invalidateAllCachesOnReload() {
        SPRITES.clear();
        RT.clear();

        int i = 0;
        while (i < GLOBAL_PART_CACHE.size()) {
            LongMeshCache<CachedMesh> shard = GLOBAL_PART_CACHE.get(i);
            synchronized (shard) {
                shard.clear();
            }
            i++;
        }
    }
}
