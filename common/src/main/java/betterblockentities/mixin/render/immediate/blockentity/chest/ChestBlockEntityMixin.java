package betterblockentities.mixin.render.immediate.blockentity.chest;

/* local */
import betterblockentities.client.gui.config.ConfigCache;
import betterblockentities.client.render.immediate.blockentity.extentions.BlockEntityExt;
import betterblockentities.client.render.immediate.blockentity.extentions.ChestBlockEntityExt;
import betterblockentities.client.render.immediate.blockentity.manager.InstancedBlockEntityManager;

/* minecraft */
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;

/* mixin */
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChestBlockEntity.class)
public abstract class ChestBlockEntityMixin implements BlockEntityExt, ChestBlockEntityExt {
    @Unique private final InstancedBlockEntityManager manager = new InstancedBlockEntityManager((BlockEntity)(Object)this);
    @Unique private BlockState cachedOtherHalfState;
    @Unique private BlockPos cachedOtherHalfPos;

    @Override public InstancedBlockEntityManager bbeManager() {
        return this.manager;
    }

    @Override
    public ChestBlockEntity bbeOtherHalf(Level level, BlockState state) {
        if (state != cachedOtherHalfState) {
            cachedOtherHalfState = state;
            cachedOtherHalfPos = null;

            if (state.getBlock() instanceof ChestBlock) {
                ChestType type = state.getValue(ChestBlock.TYPE);
                if (type != ChestType.SINGLE) {
                    Direction facing = state.getValue(ChestBlock.FACING);
                    Direction side = type == ChestType.LEFT ? facing.getClockWise() : facing.getCounterClockWise();
                    cachedOtherHalfPos = ((BlockEntity)(Object)this).getBlockPos().relative(side);
                }
            }
        }

        if (cachedOtherHalfPos == null) {
            return null;
        }

        BlockEntity other = level.getBlockEntity(cachedOtherHalfPos);
        return other instanceof ChestBlockEntity chest ? chest : null;
    }

    @Inject(method = "<init>(Lnet/minecraft/world/level/block/entity/BlockEntityType;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)V", at = @At("TAIL"))
    private void init(CallbackInfo ci) {
        BlockEntity blockEntity = (BlockEntity)(Object)this;
        BlockEntityExt ext = (BlockEntityExt)(Object)blockEntity;

        ext.optKind(InstancedBlockEntityManager.OptKind.CHEST);

        ext.supportedBlockEntity(
                blockEntity.getType() == BlockEntityType.CHEST
                        || blockEntity.getType() == BlockEntityType.TRAPPED_CHEST
        );
    }

    @Inject(method = "lidAnimateTick", at = @At("TAIL"))
    private static void onTick(Level level, BlockPos blockPos, BlockState blockState, ChestBlockEntity chestBlockEntity, CallbackInfo ci) {
        BlockEntityExt ext = (BlockEntityExt)(Object)chestBlockEntity;

        if (ext.supportedBlockEntity()) {
            ChestBlockEntityExt chestExt = (ChestBlockEntityExt)chestBlockEntity;
            ChestBlockEntity opposite = chestExt.bbeOtherHalf(level, blockState);
            boolean animating = chestBlockEntity.getOpenNess(0.5f) > 0.01f
                    || opposite != null && opposite.getOpenNess(0.5f) > 0.01f;
            chestExt.bbeManager().tick(animating, ConfigCache.chestAnims);

            if (opposite instanceof ChestBlockEntityExt oppositeExt) {
                oppositeExt.bbeManager().tick(animating, ConfigCache.chestAnims);
            }
        }
    }
}
