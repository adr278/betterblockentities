package betterblockentities.mixin.render.immediate.light;

/* local */
import betterblockentities.client.render.immediate.light.ImmediateBlockEntityLight;

/* minecraft */
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelPart;

/* mixin */
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ModelPart.class)
public class ModelPartLightPoseMixin {
    @Inject(method = "translateAndRotate", at = @At("HEAD"))
    private void bbe$applyInitialLightPose(PoseStack poseStack, CallbackInfo ci) {
        ImmediateBlockEntityLight.RenderContext context = ImmediateBlockEntityLight.active();
        if (context == null) {
            return;
        }

        ModelPart modelPart = (ModelPart)(Object)this;
        context.applyPartLightPose(
                poseStack,
                context.movingLighting() ? modelPart.storePose() : modelPart.getInitialPose()
        );
    }
}
