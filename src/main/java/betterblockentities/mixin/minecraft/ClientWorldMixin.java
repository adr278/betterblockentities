package betterblockentities.mixin.minecraft;

/* local */
import betterblockentities.chunk.ChunkUpdateDispatcher;
import betterblockentities.gui.ConfigManager;
import betterblockentities.mixin.minecraft.chest.ChestBlockEntityAccessor;
import betterblockentities.mixin.minecraft.chest.ChestLidAnimatorAccessor;
import betterblockentities.util.BlockEntityExt;
import betterblockentities.util.BlockEntityManager;
import betterblockentities.util.BlockEntityTracker;

/* fabric */
import betterblockentities.util.BlockVisibilityChecker;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/* minecraft */


/* mixin */
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.ChestLidController;
import net.minecraft.world.level.block.entity.LidBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(ClientLevel.class)
public class ClientWorldMixin {
    @Inject(method = "setServerVerifiedBlockState", at = @At("TAIL"), cancellable = true)
    public void handleBlockUpdate(BlockPos pos, BlockState blockState, int i, CallbackInfo ci) {
        if (!ConfigManager.CONFIG.master_optimize) return;

        ClientLevel world = (ClientLevel) (Object) this;
        if (!world.isClientSide()) return;

        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (blockEntity == null || !BlockEntityManager.isSupportedEntity(blockEntity)) return;

        BlockEntityExt inst = (BlockEntityExt) blockEntity;

        /* get other chest half */
        BlockEntity alt = getOtherChestHalf(blockEntity.getLevel(), blockEntity.getBlockPos());

        /* sync other half if it’s animating */
        if (alt != null && ((LidBlockEntity) alt).getOpenNess(0.5f) > 0f) {
            ChestLidController src = ((ChestBlockEntityAccessor) alt).getLidAnimator();
            ChestLidController dst = ((ChestBlockEntityAccessor) blockEntity).getLidAnimator();

            ChestLidAnimatorAccessor accSrc = (ChestLidAnimatorAccessor) src;
            ChestLidAnimatorAccessor accDst = (ChestLidAnimatorAccessor) dst;

            accDst.setOpen(accSrc.getOpen());
            accDst.setProgress(accSrc.getProgress());
            accDst.setLastProgress(accSrc.getLastProgress());

            inst.setJustReceivedUpdate(true);
        }

        /* force smart updates on other half if needed */
        else if (ConfigManager.CONFIG.updateType == 0) {
            if (alt != null) {
                BlockEntityExt altExt = (BlockEntityExt) alt;
                if (altExt.getJustReceivedUpdate() && altExt.getRemoveChunkVariant()) {
                    inst.setRemoveChunkVariant(true);
                    ChunkUpdateDispatcher.queueRebuildAtBlockPos(blockEntity.getLevel(), pos.asLong());
                    BlockEntityTracker.animMap.add(alt.getBlockPos().asLong());
                    inst.setJustReceivedUpdate(true);
                }
            }
        }
    }

    @Unique
    private static ChestBlockEntity getOtherChestHalf(Level world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);

        if (!(state.getBlock() instanceof ChestBlock)) return null;

        ChestType type = state.getValue(ChestBlock.TYPE);
        Direction facing = state.getValue(ChestBlock.FACING);

        Direction side;
        if (type == ChestType.LEFT) {
            side = facing.getClockWise();
        } else if (type == ChestType.RIGHT) {
            side = facing.getCounterClockWise();
        } else {
            return null;
        }

        BlockPos otherPos = pos.relative(side);
        BlockEntity be = world.getBlockEntity(otherPos);

        return be instanceof ChestBlockEntity ? (ChestBlockEntity) be : null;
    }
}
