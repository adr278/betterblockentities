package betterblockentities.client.chunk.pipeline.shelf;

/* minecraft */
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.ShelfBlockEntity;

public final class ShelfItemModelBuilder {
    private ShelfItemModelBuilder() {}

    private static final RenderTypeClassifier RT = new RenderTypeClassifier();
    private static final SpriteRemapper SPRITES = new SpriteRemapper();
    private static final GeometryBaker BAKER = new GeometryBaker(RT, SPRITES);

    private static final boolean STRICT_STACK_KEY = false;
    private static final boolean SKIP_GLINT_GEOMETRY = true;

    private static final int SLOT_COUNT = 3;

    @FunctionalInterface public interface PlacedPartConsumer {
        void accept(BlockStateModelPart transformedPart, GeometryBaker.LayeredPart layeredPart);
    }

    private record BakedMesh(GeometryBaker.LayeredPart[] parts, GeometryBaker.CanonicalBounds bounds) {}

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

            BakedMesh baked = bakeMesh(level, stack);
            if (baked == null) continue;

            GeometryBaker.LayeredPart[] layeredParts = baked.parts();
            BlockStateModelPart[] transformedParts = ShelfPartTransformer.transformParts(
                    layeredParts,
                    baked.bounds(),
                    slot,
                    facing,
                    alignBottom
            );

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

    private static BakedMesh bakeMesh(Level level, ItemStack stack) {
        int itemRawId = BuiltInRegistries.ITEM.getId(stack.getItem());
        int countOrZero = STRICT_STACK_KEY ? stack.getCount() : 0;

        Integer componentsHash = tryComponentsHash(stack);
        int hash = componentsHash != null ? componentsHash : System.identityHashCode(stack);
        return bake(level, stack, itemRawId, countOrZero, hash);
    }

    private static BakedMesh bake(
            Level level,
            ItemStack stack,
            int itemRawId,
            int countOrZero,
            int hash
    ) {
        CacheKeys.StackKey key = new CacheKeys.StackKey(itemRawId, countOrZero, hash);
        GeometryBaker.CanonicalMesh mesh = BAKER.bakeCanonicalMesh(level, stack, key, SKIP_GLINT_GEOMETRY);

        if (mesh.parts().length == 0) return null;

        return new BakedMesh(mesh.parts(), mesh.bounds());
    }

    private static Integer tryComponentsHash(ItemStack stack) {
        try {
            return stack.getComponents().hashCode();
        } catch (Throwable ignored) {
            return null;
        }
    }

    public static void invalidateAllCachesOnReload() {
        SPRITES.clear();
        RT.clear();
    }
}
