package betterblockentities.mixin.render.immediate.blockentity.bed;

/* local */
import betterblockentities.client.render.immediate.blockentity.extentions.BlockEntityExt;
import betterblockentities.client.render.immediate.blockentity.manager.InstancedBlockEntityManager;
import betterblockentities.client.render.immediate.blockentity.misc.RenderingMode;
import betterblockentities.client.render.immediate.util.VanillaBlockSupport;

/* minecraft */
import net.minecraft.world.level.block.entity.BedBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

/* mixin */
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BedBlockEntity.class)
public class BedBlockEntityMixin {
    @Inject(method = "<init>(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)V", at = @At("TAIL"))
    private void initTwoArg(CallbackInfo ci) {
        initialize();
    }

    @Inject(method = "<init>(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/item/DyeColor;)V", at = @At("TAIL"))
    private void initThreeArg(CallbackInfo ci) {
        initialize();
    }

    @Unique private void initialize() {
        BlockEntity blockEntity = (BlockEntity)(Object)this;
        BlockEntityExt ext = (BlockEntityExt)(Object)blockEntity;

        ext.renderingMode(RenderingMode.TERRAIN);
        ext.terrainMeshReady(true);
        ext.optKind(InstancedBlockEntityManager.OptKind.BED);

        ext.supportedBlockEntity(
                VanillaBlockSupport.isVanillaBlockEntity(blockEntity, BlockEntityType.BED)
        );
    }
}
