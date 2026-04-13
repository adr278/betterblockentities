package betterblockentities.client.chunk.pipeline;

/* local */
import betterblockentities.client.chunk.pipeline.shelf.CacheKeys;
import betterblockentities.client.chunk.pipeline.shelf.ShelfItemModelBuilder;
import betterblockentities.client.gui.config.ConfigCache;
import betterblockentities.render.AltRenderers;

/* minecraft */
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.ShelfBlock;
import net.minecraft.world.level.block.entity.ShelfBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/* sodium */
import net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderer;
import net.caffeinemc.mods.sodium.client.world.LevelSlice;

/* java */
import java.util.ArrayList;
import java.util.function.Predicate;

public final class BBEItemRenderer {
    private final BBEEmitter emitter;
    private final ArrayList<BlockStateModelPart> scratchParts = new ArrayList<>(8);

    public BBEItemRenderer(BlockRenderer sodiumBlockRenderer) { this.emitter = new BBEEmitter(sodiumBlockRenderer); }

    public void emit(
            Predicate<Direction> isFaceCulled,
            BlockAndTintGetter level,
            LevelSlice slice,
            BlockPos pos,
            BlockState state
    ) {
        if (!ConfigCache.masterOptimize || !ConfigCache.optimizeShelves || !ConfigCache.optimizeShelfItems) return;
        if (!(state.getBlock() instanceof ShelfBlock)) return;

        BlockEntityLookup lookup = findShelf(level, slice, pos);
        if (lookup.shelf == null || AltRenderers.hasRendererOverride(lookup.shelf.getType())) return;

        emitShelfItems(isFaceCulled, state, lookup.shelf, lookup.level);
    }

    private void emitShelfItems(
            Predicate<Direction> isFaceCulled,
            BlockState state,
            ShelfBlockEntity shelf,
            BlockAndTintGetter level
    ) {
        Direction facing = state.hasProperty(HorizontalDirectionalBlock.FACING)
                ? state.getValue(HorizontalDirectionalBlock.FACING)
                : Direction.NORTH;
        boolean alignBottom = shelf.getAlignItemsToBottom();

        ShelfItemModelBuilder.forEachPlacedPart(
                level,
                shelf,
                facing,
                alignBottom,
                (transformedPart, layeredPart) -> {
                    this.scratchParts.clear();
                    this.scratchParts.add(transformedPart);

                    this.emitter.clear();
                    this.emitter.setRenderType(layeredPart.layer());
                    this.emitter.setColor(layeredPart.color() == CacheKeys.NO_TINT ? -1 : layeredPart.color());
                    this.emitter.emit(this.scratchParts, isFaceCulled, this.emitter::buffer);
                }
        );

        this.scratchParts.clear();
        this.emitter.clear();
    }

    private static BlockEntityLookup findShelf(BlockAndTintGetter level, LevelSlice slice, BlockPos pos) {
        try {
            if (slice.getBlockEntity(pos) instanceof ShelfBlockEntity shelf) return new BlockEntityLookup(shelf, slice);
        } catch (Exception ignored) {}

        try {
            if (level.getBlockEntity(pos) instanceof ShelfBlockEntity shelf) return new BlockEntityLookup(shelf, level);
        } catch (Exception ignored) {}

        return BlockEntityLookup.EMPTY;
    }

    private record BlockEntityLookup(ShelfBlockEntity shelf, BlockAndTintGetter level) {
        private static final BlockEntityLookup EMPTY = new BlockEntityLookup(null, null);
    }
}
