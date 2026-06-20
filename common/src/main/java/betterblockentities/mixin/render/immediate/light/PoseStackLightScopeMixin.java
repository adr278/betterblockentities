package betterblockentities.mixin.render.immediate.light;

/* local */
import betterblockentities.client.render.immediate.light.ImmediateBlockEntityLight;

/* minecraft */
import com.mojang.blaze3d.vertex.PoseStack;

/* mixin */
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PoseStack.class)
public class PoseStackLightScopeMixin {
    @Inject(method = "pushPose", at = @At("HEAD"))
    private void bbe$pushImmediateLightFrame(CallbackInfo ci) {
        ImmediateBlockEntityLight.RenderContext context = ImmediateBlockEntityLight.active();
        if (context != null) {
            context.pushPoseFrame(this.bbe$poseStack());
        }
    }

    @Inject(method = "popPose", at = @At("HEAD"))
    private void bbe$popImmediateLightFrame(CallbackInfo ci) {
        ImmediateBlockEntityLight.RenderContext context = ImmediateBlockEntityLight.active();
        if (context != null) {
            context.popPoseFrame(this.bbe$poseStack());
        }
    }

    @Unique
    private PoseStack bbe$poseStack() {
        return (PoseStack)(Object)this;
    }
}
