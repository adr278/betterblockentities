package betterblockentities.mixin.render.immediate.blockentity.shelf;

/* local */
import betterblockentities.client.render.immediate.blockentity.BlockEntityExt;
import betterblockentities.client.render.immediate.blockentity.InstancedBlockEntityManager;

/* minecraft */
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentGetter;
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
public class ShelfBlockEntityMixin {

    @Inject(method = "<init>(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)V",
            at = @At("TAIL")
    )

    private void init(BlockPos pos, BlockState state, CallbackInfo ci) {
        BlockEntityExt ext = (BlockEntityExt)(Object)this;
        ext.supportedBlockEntity(true);
        ext.terrainMeshReady(true);
        ext.hasSpecialManager(true);
        ext.optKind(InstancedBlockEntityManager.OptKind.SHELF);
    }

    @Unique private long lastDirtyGameTime = Long.MIN_VALUE;

    @Inject(method = "loadAdditional", at = @At("TAIL"))
    private void afterLoadAdditional(ValueInput in, CallbackInfo ci) {
        dirtySectionDebounced();
    }

    @Inject(method = "applyImplicitComponents", at = @At("TAIL"))
    private void afterApplyImplicitComponents(DataComponentGetter getter, CallbackInfo ci) {
        dirtySectionDebounced();
    }

    @Inject(method = "setChanged()V", at = @At("TAIL"))
    private void afterSetChanged(CallbackInfo ci) {
        dirtySectionDebounced();
    }

    @Unique @SuppressWarnings("ConstantConditions") private void dirtySectionDebounced() {
        Level level = ((ShelfBlockEntity)(Object)this).getLevel();
        if (level == null || !level.isClientSide()) return;

        long t = level.getGameTime();
        if (t == lastDirtyGameTime) return;
        lastDirtyGameTime = t;

        Minecraft mc = Minecraft.getInstance();
        if (mc.levelRenderer == null) return;

        BlockPos p = ((ShelfBlockEntity)(Object)this).getBlockPos();
        mc.levelRenderer.setBlocksDirty(p.getX(), p.getY(), p.getZ(), p.getX(), p.getY(), p.getZ());
    }
}
