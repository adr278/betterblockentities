package betterblockentities.mixin.render.immediate.blockentity.extentions;

/* local */
import betterblockentities.client.render.immediate.blockentity.extentions.BlockEntityRenderStateExt;

/* minecraft */
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.world.level.block.entity.BlockEntity;

/* mixin */
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockEntityRenderState.class)
public class BlockEntityRenderStateMixin implements BlockEntityRenderStateExt {
    @Unique private BlockEntity blockEntity;

    @Inject(method = "extractBase", at = @At("TAIL"))
    private static void bbe$fillBaseState(BlockEntity blockEntity, BlockEntityRenderState state, ModelFeatureRenderer.CrumblingOverlay breakProgress, CallbackInfo ci) {
        BlockEntityRenderStateExt renderStateExt = (BlockEntityRenderStateExt)state;
        renderStateExt.bbe$setBlockEntity(blockEntity);
    }

    @Override public void bbe$setBlockEntity(BlockEntity blockEntity) { this.blockEntity = blockEntity; }
    @Override public BlockEntity bbe$getBlockEntity() { return this.blockEntity; }
}
