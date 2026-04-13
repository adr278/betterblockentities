package betterblockentities.client.render.immediate.blockentity.manager;

import betterblockentities.client.chunk.pipeline.shelf.ShelfItemImmediateFallback;
import betterblockentities.client.chunk.section.SectionUpdateDispatcher;
import betterblockentities.client.gui.config.ConfigCache;
import betterblockentities.client.render.immediate.blockentity.extentions.BlockEntityExt;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;

/**
 * Special cases where we might need special behavior : push the render-state at all times etc...
 */
public final class SpecialBlockEntityManager {
    private SpecialBlockEntityManager() {}

    public static boolean shouldRender(BlockEntity blockEntity) {
        Entity entity = Minecraft.getInstance().getCameraEntity();
        if (entity == null) return true;

        /* check distance to sign from player */
        if (blockEntity instanceof SignBlockEntity sign) {
            if (!ConfigCache.signText) return false;

            double maxDistSq = (double) ConfigCache.signTextRenderDistance * (double) ConfigCache.signTextRenderDistance;

            var pos = blockEntity.getBlockPos();
            double cx = pos.getX() + 0.5;
            double cy = pos.getY() + 0.5;
            double cz = pos.getZ() + 0.5;

            if (entity.distanceToSqr(cx, cy, cz) > maxDistSq) {
                return false;
            }

            SignText frontText = sign.getFrontText();
            SignText backText = sign.getBackText();

            /* prematurely check if the sign has any text at all, if not, don't proceed, we continue to cull each side inside the renderer */
            final boolean hasFront = hasAnyText(frontText, false);
            final boolean hasBack  = hasAnyText(backText, false);
            if (!hasFront && !hasBack) return false;

            return true;
        }

        /* don't continue to extract this render state if we have no items to render */
        else if (blockEntity instanceof ShelfBlockEntity shelf) {
            return ShelfItemImmediateFallback.hasShelfRendererItem(shelf);
        }
        else if (blockEntity instanceof CampfireBlockEntity campfire) {
            for (ItemStack stack : campfire.getItems()) {
                if (stack != ItemStack.EMPTY) {
                    return true;
                }
            }
            return false;
        }
        return true;
    }

    public static boolean shelfUsesSpecialManager() {
        return ShelfItemImmediateFallback.usesShelfRenderer();
    }

    public static void syncLoadedShelfSpecialManagers() {
        var minecraft = Minecraft.getInstance();
        var level = minecraft.level;
        Entity entity = minecraft.getCameraEntity();
        if (level == null || entity == null) return;

        ChunkPos center = entity.chunkPosition();
        int radius = minecraft.options.renderDistance().get() + 3;
        boolean useSpecialManager = shelfUsesSpecialManager();

        for (int chunkZ = center.z() - radius; chunkZ <= center.z() + radius; chunkZ++) {
            for (int chunkX = center.x() - radius; chunkX <= center.x() + radius; chunkX++) {
                LevelChunk chunk = level.getChunkSource().getChunk(chunkX, chunkZ, ChunkStatus.FULL, false);
                if (chunk == null) continue;

                for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
                    if (blockEntity instanceof ShelfBlockEntity shelf) {
                        BlockEntityExt ext = (BlockEntityExt) shelf;
                        if (ext.hasSpecialManager() == useSpecialManager) continue;

                        ext.hasSpecialManager(useSpecialManager);
                        SectionUpdateDispatcher.queueRebuildAtBlockPos(shelf.getBlockPos());
                    }
                }
            }
        }
    }

    public static boolean hasAnyText(SignText text, boolean filtered) {
        if (text == null) return false;
        Component[] lines = text.getMessages(filtered);
        for (int i = 0; i < 4; i++) {
            if (!lines[i].getString().isEmpty()) return true;
        }
        return false;
    }
}
