package betterblockentities.client.render.immediate.blockentity.misc;

/* local */
import betterblockentities.client.chunk.section.SectionUpdateDispatcher;
import betterblockentities.client.gui.config.ConfigCache;
import betterblockentities.client.render.immediate.blockentity.extentions.BlockEntityExt;
import betterblockentities.client.render.immediate.util.BlockVisibilityChecker;
import betterblockentities.mixin.accessors.ChestBlockEntityAccessor;
import betterblockentities.mixin.accessors.ChestLidControllerAccessor;

/* minecraft */
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.ChestLidController;
import net.minecraft.world.level.block.state.BlockState;

public class LidControllerSync {
    public static void sync(ClientLevel clientLevel, BlockPos blockPos, BlockState blockState) {
        if (!ConfigCache.optimizeChests || !clientLevel.isClientSide()) return;

        final Block block = blockState.getBlock();
        if (!(block instanceof ChestBlock)) return;

        BlockEntity blockEntity = tryGetBlockEntity(clientLevel, blockPos);
        ChestBlockEntity opposite = BlockVisibilityChecker.getOtherChestHalf(clientLevel, blockPos);

        if (blockEntity == null || opposite == null) return;

        syncRenderingModes(blockEntity, opposite);

        if (!ConfigCache.chestAnims || !(opposite.getOpenNess(0.5f) > 0f)) return;

        /* sync over the lid controller from the animating half. this also auto schedules a manager for us */
        ChestLidController src = ((ChestBlockEntityAccessor)opposite).bbe$getLidController();
        ChestLidController dst = ((ChestBlockEntityAccessor)blockEntity).bbe$getLidController();

        ChestLidControllerAccessor accSrc = (ChestLidControllerAccessor)src;
        ChestLidControllerAccessor accDst = (ChestLidControllerAccessor)dst;

        accDst.bbe$setOpen(accSrc.bbe$getOpen());
        accDst.bbe$setProgress(accSrc.bbe$getProgress());
        accDst.bbe$setLastProgress(accSrc.bbe$getLastProgress());

        /* sketchy, force a triggerEvent to wake up the block entity ticker (lithium workaround) */
        clientLevel.blockEvent(blockPos, blockState.getBlock(), 1, 0);

    }

    /** Keeps added half chest on the immediate render path if the original half is immediate. */
    private static void syncRenderingModes(BlockEntity blockEntity, ChestBlockEntity opposite) {
        BlockEntityExt blockEntityExt = (BlockEntityExt)blockEntity;
        BlockEntityExt oppositeExt = (BlockEntityExt)opposite;

        if (blockEntityExt.bbe$getRenderingMode() != RenderingMode.IMMEDIATE && oppositeExt.bbe$getRenderingMode() != RenderingMode.IMMEDIATE) {
            return;
        }

        setImmediate(blockEntity, blockEntityExt);
        setImmediate(opposite, oppositeExt);
    }

    private static void setImmediate(BlockEntity blockEntity, BlockEntityExt ext) {
        boolean requiresRebuild = ext.bbe$getRenderingMode() != RenderingMode.IMMEDIATE || ext.bbe$isTerrainMeshReady();

        ext.bbe$setTerrainMeshReady(false);
        ext.bbe$setRenderingMode(RenderingMode.IMMEDIATE);

        if (requiresRebuild) {
            SectionUpdateDispatcher.queueRebuildAtBlockPos(blockEntity.getBlockPos());
        }
    }

    private static BlockEntity tryGetBlockEntity(Level level, BlockPos pos) {
        try {
            return level.getBlockEntity(pos);
        } catch (Exception e) {
            return null;
        }
    }
}
