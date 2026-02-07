package betterblockentities.client.render.immediate.blockentity;

/* local */
import betterblockentities.client.gui.config.ConfigCache;

/* minecraft */
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ShelfBlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;

/**
 * Special cases where we might need special behavior : push the render-state at all times etc...
 */
public final class SpecialBlockEntityManager {
    private SpecialBlockEntityManager() {}

    public static boolean shouldRender(BlockEntity blockEntity) {
        Entity entity = Minecraft.getInstance().getCameraEntity();
        if (entity == null) return true;

        if (blockEntity instanceof ShelfBlockEntity shelf
                && ConfigCache.masterOptimize
                && ConfigCache.optimizeShelf) {

            if (!shelfHasAnyItem(shelf)) return false;
        }

        if (blockEntity instanceof SignBlockEntity) {
            if (!ConfigCache.signText) return false;

            double maxDistSq = (double) ConfigCache.signTextRenderDistance * (double) ConfigCache.signTextRenderDistance;

            var pos = blockEntity.getBlockPos();
            double cx = pos.getX() + 0.5;
            double cy = pos.getY() + 0.5;
            double cz = pos.getZ() + 0.5;

            return entity.distanceToSqr(cx, cy, cz) < maxDistSq;
        }
        return true;
    }

    private static boolean shelfHasAnyItem(ShelfBlockEntity shelf) {
        var items = shelf.getItems();
        for (int i = 0; i < ShelfBlockEntity.MAX_ITEMS; i++) {
            if (!items.get(i).isEmpty()) return true;
        }
        return false;
    }
}
