package betterblockentities.client.chunk.pipeline.shelf;

/* local */
import betterblockentities.client.render.immediate.blockentity.ShelfSlotMeshCacheAccess;

/* minecraft */
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ShelfBlock;
import net.minecraft.world.level.block.entity.ShelfBlockEntity;

/* java/misc */
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import static java.util.Arrays.copyOf;

public final class ShelfItemModelBuilder {
    private ShelfItemModelBuilder() {}

    private static final RenderTypeClassifier RT = new RenderTypeClassifier();
    private static final SpriteRemapper SPRITES = new SpriteRemapper();
    private static final GeometryBaker BAKER = new GeometryBaker(RT, SPRITES);

    private static final boolean STRICT_STACK_KEY = false;
    private static final boolean SKIP_GLINT_GEOMETRY = true;

    private record SlotMesh(long sig0, long sig1, GeometryBaker.LayeredPart[] parts) {}

    private static final ThreadLocal<ThreadPartCache> TL_PART_CACHE =
            ThreadLocal.withInitial(ThreadPartCache::new);

    /**
     * Global epoch; bump to invalidate all per-thread caches.
     */
    private static final int MODEL_CACHE_CAPACITY_PER_THREAD = 1024;
    private static final AtomicInteger MODEL_CACHE_EPOCH = new AtomicInteger(0);

    private static final class ThreadPartCache {
        int epoch = MODEL_CACHE_EPOCH.get();
        final LongMeshCache<GeometryBaker.LayeredPart[]> map =
                new LongMeshCache<>(MODEL_CACHE_CAPACITY_PER_THREAD);
        void refreshEpoch() {
            int e = MODEL_CACHE_EPOCH.get();
            if (epoch != e) {
                epoch = e;
                map.clear();
            }
        }
    }

    public static List<GeometryBaker.LayeredPart> getParts(BlockAndTintGetter levelView, ShelfBlockEntity shelf) {
        Level level = (levelView instanceof Level l) ? l : shelf.getLevel();
        if (level == null) return Collections.emptyList();
        return getParts(level, shelf);
    }

    public static List<GeometryBaker.LayeredPart> getParts(Level level, ShelfBlockEntity shelf) {
        final ThreadPartCache cache = TL_PART_CACHE.get();
        cache.refreshEpoch();

        final int epoch = MODEL_CACHE_EPOCH.get();
        final ShelfSlotMeshCacheAccess slotCache = (ShelfSlotMeshCacheAccess) shelf;

        Direction facing = shelf.getBlockState().getValue(ShelfBlock.FACING);
        final int facing2d = CacheKeys.facing2d(facing);
        final boolean alignBottom = shelf.getAlignItemsToBottom();

        ArrayList<GeometryBaker.LayeredPart> out = new ArrayList<>(6);

        for (int slot = 0; slot < 3; slot++) {
            ItemStack stack = shelf.getItems().get(slot);
            if (stack.isEmpty()) continue;

            final int itemRawId = BuiltInRegistries.ITEM.getId(stack.getItem());
            final int componentsHash = safeComponentsHash(stack);
            final int countOrZero = STRICT_STACK_KEY ? stack.getCount() : 0;

            final long sig0 = CacheKeys.packSig0(itemRawId, componentsHash);
            final long sig1 = CacheKeys.packSig1WithSlot(countOrZero, facing2d, alignBottom, slot);
            final long key = CacheKeys.mix64(sig0, sig1);

            // L1: per-shelf slot cache.
            Object cachedObj = slotCache.getSlotMesh(slot, epoch, sig0, sig1);
            if (cachedObj instanceof SlotMesh(
                    long sig2, long sig3, GeometryBaker.LayeredPart[] parts
            ) && sig2 == sig0 && sig3 == sig1) {
                Collections.addAll(out, parts);
                continue;
            }

            // L2: per-thread cache.
            GeometryBaker.LayeredPart[] bakedParts = cache.map.get(key);

            if (bakedParts == null) {
                CacheKeys.StackKey sk = new CacheKeys.StackKey(itemRawId, countOrZero, componentsHash);

                GeometryBaker.LayeredPart[] produced =
                        BAKER.bakeLayeredParts(level, shelf, slot, stack, 0, sk, SKIP_GLINT_GEOMETRY);

                if (produced.length == 0) continue;

                // Defensive copy so the cached array can’t be reused accidentally.
                bakedParts = copyOf(produced, produced.length);

                cache.map.put(key, bakedParts);
            } else {
                bakedParts = copyOf(bakedParts, bakedParts.length);
            }

            SlotMesh wrapped = new SlotMesh(sig0, sig1, bakedParts);
            slotCache.putSlotMesh(slot, epoch, sig0, sig1, wrapped);

            Collections.addAll(out, bakedParts);
        }

        return out;
    }

    private static int safeComponentsHash(ItemStack stack) {
        try {
            return stack.getComponents().hashCode();
        } catch (Throwable t) {
            return 0;
        }
    }

    public static void invalidateAllCachesOnReload() {
        synchronized (ShelfItemModelBuilder.class) {
            // Clear helper caches
            SPRITES.clear();
            RT.clear();
            // Invalidate all per-thread caches.
            MODEL_CACHE_EPOCH.incrementAndGet();
        }
    }
}
