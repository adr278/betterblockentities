package betterblockentities.mixin.render.immediate.blockentity.banner;

/* local */
import betterblockentities.client.render.immediate.blockentity.extentions.BlockEntityExt;
import betterblockentities.client.render.immediate.blockentity.manager.InstancedBlockEntityManager;
import betterblockentities.client.render.immediate.blockentity.misc.RenderingMode;
import betterblockentities.client.render.immediate.util.VanillaBlockSupport;

/* minecraft */
import net.minecraft.world.level.block.entity.BannerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

/* mixin */
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BannerBlockEntity.class)
public class BannerBlockEntityMixin {
    @Inject(method = "<init>(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/item/DyeColor;)V", at = @At("TAIL"))
    private void init(CallbackInfo ci) {
        BlockEntity blockEntity = (BlockEntity)(Object)this;
        BlockEntityExt ext = (BlockEntityExt)(Object)blockEntity;

        ext.renderingMode(RenderingMode.TERRAIN);
        ext.terrainMeshReady(true);
        ext.optKind(InstancedBlockEntityManager.OptKind.BANNER);

        ext.supportedBlockEntity(
                VanillaBlockSupport.isVanillaBlockEntity(blockEntity, BlockEntityType.BANNER)
        );
    }
}
