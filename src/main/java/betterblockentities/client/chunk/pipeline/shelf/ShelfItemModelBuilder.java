package betterblockentities.client.chunk.pipeline.shelf;

/* minecraft */
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.ShelfBlockEntity;

/* java */
import java.util.Arrays;

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

    private static final GeometryBaker.LayeredPart[] NO_PARTS = new GeometryBaker.LayeredPart[0];
    private static final GeometryBaker.CanonicalBounds ZERO_BOUNDS =
            new GeometryBaker.CanonicalBounds(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);

    private record CacheShard(LongMeshCache<CachedMesh> cache, Object lock) {}

    private static final CacheShard[] GLOBAL_PART_CACHE = createShards();

    @FunctionalInterface public interface PlacedPartConsumer {
        void accept(BlockModelPart transformedPart, GeometryBaker.LayeredPart layeredPart);
    }

    private record CachedMesh(
            long sig0,
            long sig1,
            GeometryBaker.LayeredPart[] parts,
            GeometryBaker.CanonicalBounds bounds,
            BlockModelPart[][] transformedVariants
    ) {
        private static CachedMesh empty(long sig0, long sig1) {
            return new CachedMesh(
                    sig0, sig1,
                    NO_PARTS,
                    ZERO_BOUNDS,
                    new BlockModelPart[ShelfPartTransformer.VARIANT_COUNT][]
            );
        }

        private boolean isEmpty() {
            return parts.length == 0;
        }

        private BlockModelPart[] getVariantParts(int slot, Direction facing, boolean alignBottom) {
            int variantIndex = ShelfPartTransformer.variantIndex(slot, facing, alignBottom);

            BlockModelPart[] hit = transformedVariants[variantIndex];
            if (hit != null) {
                return hit;
            }
            synchronized (this) {
                hit = transformedVariants[variantIndex];
                if (hit != null) {
                    return hit;
                }

                BlockModelPart[] created = ShelfPartTransformer.transformParts(
                        parts,
                        bounds,
                        slot,
                        facing,
                        alignBottom
                );
                transformedVariants[variantIndex] = created;
                return created;
            }
        }
    }

    private static CacheShard[] createShards() {
        CacheShard[] shards = new CacheShard[GLOBAL_CACHE_SHARDS];
        for (int i = 0; i < GLOBAL_CACHE_SHARDS; i++) {
            shards[i] = new CacheShard(new LongMeshCache<>(SHARD_CAPACITY), new Object());
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
        Level level = (levelView instanceof Level l) ? l : shelf.getLevel();
        if (level == null) {
            return;
        }
        forEachPlacedPart(level, shelf, facing, alignBottom, consumer);
    }

    public static void forEachPlacedPart(
            Level level,
            ShelfBlockEntity shelf,
            Direction facing,
            boolean alignBottom,
            PlacedPartConsumer consumer
    ) {
        for (int slot = 0; slot < 3; slot++) {
            ItemStack stack = shelf.getItems().get(slot);
            if (stack.isEmpty()) {
                continue;
            }
            CachedMesh cached = getOrBakeCachedMesh(level, stack);
            if (cached == null || cached.isEmpty()) {
                continue;
            }
            GeometryBaker.LayeredPart[] layeredParts = cached.parts();
            BlockModelPart[] transformedParts = cached.getVariantParts(slot, facing, alignBottom);
            for (int i = 0; i < layeredParts.length; i++) {
                consumer.accept(transformedParts[i], layeredParts[i]);
            }
        }
    }

    private static CachedMesh getOrBakeCachedMesh(Level level, ItemStack stack) {
        final int itemRawId = BuiltInRegistries.ITEM.getId(stack.getItem());
        final int countOrZero = STRICT_STACK_KEY ? stack.getCount() : 0;
        final Integer componentsHash = tryComponentsHash(stack);

        if (componentsHash == null) {
            return bakeMeshUncached(level, stack, itemRawId, countOrZero);
        }

        final long sig0 = CacheKeys.packSig0(itemRawId, componentsHash);
        final long sig1 = CacheKeys.packSig1(countOrZero);
        final long key = CacheKeys.mix64(sig0, sig1);

        final CacheShard shard = GLOBAL_PART_CACHE[shardIndex(key)];

        synchronized (shard.lock()) {
            CachedMesh hit = shard.cache().get(key);
            if (hit != null && hit.sig0() == sig0 && hit.sig1() == sig1) {
                return hit.isEmpty() ? null : hit;
            }
            CachedMesh baked = bakeMesh(level, stack, itemRawId, componentsHash, countOrZero, sig0, sig1);
            shard.cache().put(key, baked);
            return baked.isEmpty() ? null : baked;
        }
    }

    private static CachedMesh bakeMeshUncached(
            Level level,
            ItemStack stack,
            int itemRawId,
            int countOrZero
    ) {
        CacheKeys.StackKey sk = new CacheKeys.StackKey(
                itemRawId,
                countOrZero,
                System.identityHashCode(stack)
        );

        GeometryBaker.CanonicalMesh produced = BAKER.bakeCanonicalMesh(level, stack, sk, SKIP_GLINT_GEOMETRY);
        if (produced.parts().length == 0) {
            return null;
        }
        return new CachedMesh(
                0L, 0L,
                Arrays.copyOf(produced.parts(), produced.parts().length),
                produced.bounds(),
                new BlockModelPart[ShelfPartTransformer.VARIANT_COUNT][]
        );
    }

    private static CachedMesh bakeMesh(
            Level level,
            ItemStack stack,
            int itemRawId,
            int componentsHash,
            int countOrZero,
            long sig0,
            long sig1
    ) {
        CacheKeys.StackKey sk = new CacheKeys.StackKey(itemRawId, countOrZero, componentsHash);
        GeometryBaker.CanonicalMesh produced = BAKER.bakeCanonicalMesh(level, stack, sk, SKIP_GLINT_GEOMETRY);

        if (produced.parts().length == 0) {
            return CachedMesh.empty(sig0, sig1);
        }
        return new CachedMesh(
                sig0, sig1,
                Arrays.copyOf(produced.parts(), produced.parts().length),
                produced.bounds(),
                new BlockModelPart[ShelfPartTransformer.VARIANT_COUNT][]
        );
    }

    private static Integer tryComponentsHash(ItemStack stack) {
        try {
            return stack.getComponents().hashCode();
        } catch (Throwable t) {
            return null;
        }
    }

    private static int shardIndex(long key) {
        return (int) (CacheKeys.mix64(key, key ^ 0x9E3779B97F4A7C15L) & (GLOBAL_CACHE_SHARDS - 1));
    }

    public static void invalidateAllCachesOnReload() {
        // Invalidate all cache at reload.
        SPRITES.clear();
        RT.clear();
        for (CacheShard shard : GLOBAL_PART_CACHE) {
            synchronized (shard.lock()) {
                shard.cache().clear();
            }
        }
    }
}
