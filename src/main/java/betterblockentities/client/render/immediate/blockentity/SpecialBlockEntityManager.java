package betterblockentities.client.render.immediate.blockentity;

import betterblockentities.client.gui.config.ConfigCache;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;

/**
 * Special cases where we might need special behavior : push the render-state at all times etc...
 */
public final class SpecialBlockEntityManager {
    private SpecialBlockEntityManager() {}

    public static boolean shouldRender(BlockEntity blockEntity) {
        Entity entity = Minecraft.getInstance().getCameraEntity();
        if (entity == null) return true;

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
}
