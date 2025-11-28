package betterblockentities.mixin.minecraft.sign;

/* local */
import betterblockentities.gui.ConfigManager;

/* minecraft */


/* mixin */
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.AbstractSignRenderer;
import net.minecraft.client.renderer.blockentity.state.SignRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.SignBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/*
    this whole mixin will probably get removed once we move to baking the sign text into meshes
    as this implementation is not as efficient
*/
@Mixin(AbstractSignRenderer.class)
public abstract class AbstractSignBlockEntityRendererMixin {
    @Shadow protected abstract void translateSign(PoseStack matrices, float blockRotationDegrees, BlockState state);
    @Shadow protected abstract void submitSignText(SignRenderState renderState, PoseStack matrices, SubmitNodeCollector queue, boolean front);

    @Inject(method = "submit(Lnet/minecraft/client/renderer/blockentity/state/SignRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/CameraRenderState;)V", at = @At("HEAD"), cancellable = true)
    public void render(SignRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState cameraRenderState, CallbackInfo ci) {
        if (!ConfigManager.CONFIG.optimize_signs || !ConfigManager.CONFIG.master_optimize) return;

        ci.cancel();

        /* sanity check */
        if (state.frontText == null || state.backText == null) return;

        /* check if we have text */
        boolean hasTextFront = hasText(state.frontText.getMessages(false));
        boolean hasTextBack = hasText(state.backText.getMessages(false));

        /* if no text then don't render */
        if (!hasTextFront && !hasTextBack) return;

        BlockState blockState = state.blockState;
        SignBlock block = (SignBlock) blockState.getBlock();

        poseStack.pushPose();
        this.translateSign(poseStack, -block.getYRotationDegrees(blockState), blockState);

        if (hasTextFront) this.submitSignText(state, poseStack, submitNodeCollector, true);
        if (hasTextBack)  this.submitSignText(state, poseStack, submitNodeCollector, false);

        poseStack.popPose();
    }

    @Unique
    private boolean hasText(Component[] lines) {
        for (Component line : lines) {
            if (line != null && !line.getString().isEmpty()) return true;
        }
        return false;
    }
}
