package betterblockentities.mixin.minecraft;

/* local */
import betterblockentities.gui.ConfigManager;
import betterblockentities.util.BlockEntityManager;

/* minecraft */
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.SubmitNodeStorage;

/* mixin */
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/*
    quick and dirty fix for chest minecarts and endermen holding any supported block where there is two BEs rendered
    just removes this line in BatchingRenderCommandQueue -> submitBlock:
    -this.blockCommands.add(new OrderedRenderCommandQueueImpl.BlockCommand(matrices.peek().copy(), state, light, overlay, outlineColor));
    which loads a predefined model (from the block´s blockstate json) aka our defined chest model in our pack
*/

@Mixin(SubmitNodeCollection.class)
public class OrderedRenderCommandQueueImplMixin {
    @Shadow @Final private SubmitNodeStorage submitNodeStorage;

    @Inject(method = "submitBlock", at = @At("HEAD"), cancellable = true)
    public void submitBlock(PoseStack poseStack, net.minecraft.world.level.block.state.BlockState state, int i, int j, int k, CallbackInfo ci) {
        if (BlockEntityManager.isSupportedBlock(state.getBlock()) && ConfigManager.CONFIG.master_optimize) {
            ci.cancel();
            Minecraft.getInstance()
                    .getModelManager()
                    .specialBlockModelRenderer()
                    .renderByBlock(state.getBlock(), net.minecraft.world.item.ItemDisplayContext.NONE, poseStack, this.submitNodeStorage, i, j, k);
        }
    }
}
