package betterblockentities.mixin.render.immediate.blockentity;

/* local */
import betterblockentities.client.gui.config.ConfigCache;
import betterblockentities.client.render.immediate.blockentity.extentions.BlockEntityExt;
import betterblockentities.client.render.immediate.blockentity.manager.InstancedBlockEntityManager;

/* minecraft */
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTypes;
import net.minecraft.world.level.block.entity.EnderChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/* mixin */
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EnderChestBlockEntity.class)
public abstract class EnderChestBlockEntityMixin {
    @Unique private final InstancedBlockEntityManager manager = new InstancedBlockEntityManager((BlockEntity)(Object)this);

    @Inject(method = "<init>", at = @At("TAIL"))
    private void bbe$init(CallbackInfo ci) {
        BlockEntity blockEntity = (BlockEntity)(Object)this;
        BlockEntityExt ext = (BlockEntityExt)(Object)blockEntity;

        ext.bbe$setOptKind(InstancedBlockEntityManager.OptKind.CHEST);

        ext.bbe$setSupportedBlockEntity(
                blockEntity.getType() == BlockEntityTypes.CHEST         ||
                blockEntity.getType() == BlockEntityTypes.TRAPPED_CHEST ||
                blockEntity.getType() == BlockEntityTypes.ENDER_CHEST
        );
    }

    @Inject(method = "lidAnimateTick", at = @At("TAIL"))
    private static void bbe$onTick(Level level, BlockPos blockPos, BlockState blockState, EnderChestBlockEntity enderChestBlockEntity, CallbackInfo ci) {
        EnderChestBlockEntityMixin self = (EnderChestBlockEntityMixin)(Object)enderChestBlockEntity;
        BlockEntityExt ext = (BlockEntityExt)(Object)enderChestBlockEntity;

        if (ext.bbe$isSupportedBlockEntity()) {
            self.manager.tick(enderChestBlockEntity.getOpenNess(0.5f) > 0.01f, ConfigCache.chestAnims);
        }
    }
}
