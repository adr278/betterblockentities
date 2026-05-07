package betterblockentities.mixin.render.immediate.blockentity.shelf;

import betterblockentities.client.render.immediate.blockentity.extentions.BlockEntityExt;
import betterblockentities.client.render.immediate.blockentity.manager.InstancedBlockEntityManager;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.ShelfBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ShelfBlockEntity.class)
public class ShelfBlockEntityMixin {
    @Inject(method = "<init>", at = @At("TAIL"))
    private void init(CallbackInfo ci) {
        BlockEntity blockEntity = (BlockEntity)(Object)this;
        BlockEntityExt ext = (BlockEntityExt)(Object)blockEntity;

        ext.terrainMeshReady(true);
        ext.hasSpecialManager(true);
        ext.optKind(InstancedBlockEntityManager.OptKind.SHELF);

        ext.supportedBlockEntity(
                blockEntity.getType() == BlockEntityType.SHELF
        );
    }
}
