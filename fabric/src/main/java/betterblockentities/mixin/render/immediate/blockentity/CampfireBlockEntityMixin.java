package betterblockentities.mixin.render.immediate.blockentity;

/* local */
import betterblockentities.client.render.immediate.blockentity.extentions.BlockEntityExt;
import betterblockentities.client.render.immediate.blockentity.manager.InstancedBlockEntityManager;

/* minecraft */
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTypes;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;

/* mixin */
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CampfireBlockEntity.class)
public class CampfireBlockEntityMixin {
    @Inject(method = "<init>", at = @At("TAIL"))
    private void bbe$init(CallbackInfo ci) {
        BlockEntity blockEntity = (BlockEntity)(Object)this;
        BlockEntityExt ext = (BlockEntityExt)(Object)blockEntity;

        ext.bbe$setTerrainMeshReady(true);
        ext.bbe$setSpecialManager(true);
        ext.bbe$setOptKind(InstancedBlockEntityManager.OptKind.CAMPFIRE);

        ext.bbe$setSupportedBlockEntity(
            blockEntity.getType() == BlockEntityTypes.CAMPFIRE
        );
    }
}
