package betterblockentities.mixin.render.immediate.blockentity.shelf;

/* local */
import betterblockentities.client.chunk.section.SectionUpdateDispatcher;
import betterblockentities.client.render.immediate.blockentity.BlockEntityExt;
import betterblockentities.client.render.immediate.blockentity.InstancedBlockEntityManager;

/* minecraft */
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.ShelfBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;

/* mixin */
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ShelfBlockEntity.class)
@SuppressWarnings("unused") public class ShelfBlockEntityMixin {

    @Inject(method = "<init>(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)V", at = @At("TAIL"))
    private void init(BlockPos pos, BlockState state, CallbackInfo ci) {
        BlockEntityExt ext = (BlockEntityExt) (Object) this;
        ext.supportedBlockEntity(true);
        ext.terrainMeshReady(true);
        ext.optKind(InstancedBlockEntityManager.OptKind.SHELF);
    }

    @Inject(method = "setChanged()V", at = @At("TAIL"))
    private void afterSetChanged(CallbackInfo ci) {
        dirtySectionDebounced();
    }

    @Inject(method = "loadAdditional", at = @At("TAIL"))
    private void afterLoadAdditional(ValueInput in, CallbackInfo ci) {
        dirtySectionDebounced();
    }

    @Unique private Level lastLevel = null;
    @Unique private long lastDirtyGameTime = Long.MIN_VALUE;

    @Unique @SuppressWarnings("ConstantConditions") private void dirtySectionDebounced() {
        ShelfBlockEntity self = (ShelfBlockEntity) (Object) this;
        Level level = self.getLevel();
        if (level == null || !level.isClientSide()) return;

        SectionUpdateDispatcher.queueRebuildAtBlockPos(self.getBlockPos());
    }
}
