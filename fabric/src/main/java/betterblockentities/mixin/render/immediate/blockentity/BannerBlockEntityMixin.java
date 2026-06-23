package betterblockentities.mixin.render.immediate.blockentity;

/* local */
import betterblockentities.client.render.immediate.blockentity.extentions.BlockEntityExt;
import betterblockentities.client.render.immediate.blockentity.manager.InstancedBlockEntityManager;
import betterblockentities.client.render.immediate.blockentity.misc.RenderingMode;

/* minecraft */
import net.minecraft.world.level.block.entity.BannerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTypes;

/* mixin */
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BannerBlockEntity.class)
public class BannerBlockEntityMixin {
    @Inject(method = "<init>(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/item/DyeColor;)V", at = @At("TAIL"))
    private void bbe$init(CallbackInfo ci) {
        BlockEntity blockEntity = (BlockEntity)(Object)this;
        BlockEntityExt ext = (BlockEntityExt)(Object)blockEntity;

        ext.bbe$setRenderingMode(RenderingMode.TERRAIN);
        ext.bbe$setTerrainMeshReady(true);
        ext.bbe$setOptKind(InstancedBlockEntityManager.OptKind.BANNER);

        ext.bbe$setSupportedBlockEntity(
            blockEntity.getType() == BlockEntityTypes.BANNER
        );
    }
}
