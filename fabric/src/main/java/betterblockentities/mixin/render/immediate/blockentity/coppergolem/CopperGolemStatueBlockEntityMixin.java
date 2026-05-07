package betterblockentities.mixin.render.immediate.blockentity.coppergolem;

/* local */
import betterblockentities.client.render.immediate.blockentity.extentions.BlockEntityExt;
import betterblockentities.client.render.immediate.blockentity.manager.InstancedBlockEntityManager;
import betterblockentities.client.render.immediate.blockentity.misc.RenderingMode;

/* minecraft */
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.CopperGolemStatueBlockEntity;

/* mixin */
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CopperGolemStatueBlockEntity.class)
public class CopperGolemStatueBlockEntityMixin {
    @Inject(method = "<init>", at = @At("TAIL"))
    private void init(CallbackInfo ci) {
        BlockEntity blockEntity = (BlockEntity)(Object)this;
        BlockEntityExt ext = (BlockEntityExt)(Object)blockEntity;

        ext.renderingMode(RenderingMode.TERRAIN);
        ext.terrainMeshReady(true);
        ext.optKind(InstancedBlockEntityManager.OptKind.CGS);

        ext.supportedBlockEntity(
            blockEntity.getType() == BlockEntityType.COPPER_GOLEM_STATUE
        );
    }
}
