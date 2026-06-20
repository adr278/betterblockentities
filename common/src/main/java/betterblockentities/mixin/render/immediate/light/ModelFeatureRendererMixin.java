package betterblockentities.mixin.render.immediate.light;

/* local */
import betterblockentities.client.render.immediate.light.ImmediateBlockEntityLight;
import betterblockentities.client.render.immediate.light.ImmediateLightSubmitExt;

/* minecraft */
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;

/* mixin */
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ModelFeatureRenderer.class)
public class ModelFeatureRendererMixin {
    @WrapOperation(
            method = "prepareModel",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/model/Model;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V"
            )
    )
    private <S> void bbe$renderWithImmediateLight(
            Model<S> model,
            PoseStack poseStack,
            VertexConsumer vertexConsumer,
            int lightCoords,
            int overlayCoords,
            int color,
            Operation<Void> original,
            @Local(argsOnly = true) ModelFeatureRenderer.Submit<S> submit
    ) {
        ImmediateBlockEntityLight.Parameters parameters = ((ImmediateLightSubmitExt)(Object)submit).bbe$getLightParameters();
        try (ImmediateBlockEntityLight.Scope ignored = ImmediateBlockEntityLight.pushActive(parameters)) {
            original.call(model, poseStack, vertexConsumer, lightCoords, overlayCoords, color);
        }
    }
}
