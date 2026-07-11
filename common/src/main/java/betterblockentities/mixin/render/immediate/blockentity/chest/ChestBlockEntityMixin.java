package betterblockentities.mixin.render.immediate.blockentity.chest;

/* local */
import betterblockentities.client.gui.config.ConfigCache;
import betterblockentities.client.render.immediate.blockentity.extentions.BlockEntityExt;
import betterblockentities.client.render.immediate.blockentity.extentions.ChestBlockEntityExt;
import betterblockentities.client.render.immediate.blockentity.manager.InstancedBlockEntityManager;
import betterblockentities.client.render.immediate.util.BlockVisibilityChecker;

/* minecraft */
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/* mixin */
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChestBlockEntity.class)
public abstract class ChestBlockEntityMixin implements BlockEntityExt, ChestBlockEntityExt {
    @Unique private final InstancedBlockEntityManager manager = new InstancedBlockEntityManager((BlockEntity)(Object)this);

    @Override public InstancedBlockEntityManager bbeManager() {
        return this.manager;
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
            boolean animating = isAnimating(chestBlockEntity, level, blockPos);
            ((ChestBlockEntityExt) chestBlockEntity).bbeManager().tick(animating, ConfigCache.chestAnims);

            ChestBlockEntity opposite = BlockVisibilityChecker.getOtherChestHalf(level, blockPos);
            if (opposite instanceof ChestBlockEntityExt oppositeExt) {
                oppositeExt.bbeManager().tick(animating, ConfigCache.chestAnims);
            }
        }
    }

    @Unique private static boolean isAnimating(ChestBlockEntity chestBlockEntity, Level level, BlockPos pos) {
        if (chestBlockEntity.getOpenNess(0.5f) > 0.01f) return true;

        ChestBlockEntity opposite = BlockVisibilityChecker.getOtherChestHalf(level, pos);
        return opposite != null && opposite.getOpenNess(0.5f) > 0.01f;
    }
}
