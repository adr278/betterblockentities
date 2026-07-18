package betterblockentities.client.render.immediate.blockentity.manager;

import betterblockentities.client.gui.config.ConfigCache;
import betterblockentities.client.render.immediate.blockentity.extentions.CampfireBlockEntityExt;
import betterblockentities.client.render.immediate.blockentity.extentions.LecternBlockEntityExt;
import betterblockentities.client.render.immediate.blockentity.extentions.SignBlockEntityExt;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.*;

import java.util.Optional;

/**
 * Special cases where we might need special behavior : push the render-state at all times etc...
 */
public final class SpecialBlockEntityManager {
    private static final Optional<Boolean> TEXT_FOUND = Optional.of(Boolean.TRUE);

    private SpecialBlockEntityManager() {}

    public static boolean shouldRender(BlockEntity blockEntity) {
        /* check distance to sign from player */
        if (blockEntity instanceof SignBlockEntity sign) {
            if (!ConfigCache.signText) return false;

            /* prematurely check if the sign has any text at all, if not, don't proceed, we continue to cull each side inside the renderer */
            if (!((SignBlockEntityExt)sign).hasAnyText()) return false;

            Entity entity = Minecraft.getInstance().getCameraEntity();
            if (entity == null) return true;

            double maxDistSq = (double) ConfigCache.signTextRenderDistance * (double) ConfigCache.signTextRenderDistance;

            var pos = blockEntity.getBlockPos();
            double cx = pos.getX() + 0.5;
            double cy = pos.getY() + 0.5;
            double cz = pos.getZ() + 0.5;

            if (entity.distanceToSqr(cx, cy, cz) > maxDistSq) {
                return false;
            }

            return true;
        }

        /* don't continue to extract this render state if we have no items to render */
        else if (blockEntity instanceof CampfireBlockEntity campfire) {
            return ((CampfireBlockEntityExt)campfire).hasRenderableItems();
        }

        else if (blockEntity instanceof LecternBlockEntity lectern) {
            return ((LecternBlockEntityExt)lectern).hasBookForRendering();
        }

        return true;
    }

    public static boolean hasAnyText(SignText text, boolean filtered) {
        if (text == null) return false;
        Component[] lines = text.getMessages(filtered);
        for (int i = 0; i < 4; i++) {
            Component line = lines[i];
            String collapsed = line.tryCollapseToString();
            if (collapsed != null) {
                if (!collapsed.isEmpty()) return true;
            } else if (line.visit(string -> string.isEmpty() ? Optional.empty() : TEXT_FOUND).isPresent()) {
                return true;
            }
        }
        return false;
    }
}
