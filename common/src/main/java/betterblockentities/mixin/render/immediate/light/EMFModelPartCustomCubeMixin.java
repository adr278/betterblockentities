package betterblockentities.mixin.render.immediate.light;

/* local */
import betterblockentities.client.render.immediate.light.ImmediateBlockEntityLight;

/* mojang */
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

/* minecraft */
import net.minecraft.client.model.geom.ModelPart;

/* mixin */
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

@Pseudo
@Mixin(targets = "traben.entity_model_features.models.parts.EMFModelPartCustom$EMFCube", remap = false)
public class EMFModelPartCustomCubeMixin {
    @WrapOperation(
            method = "compile",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/vertex/VertexConsumer;addVertex(FFFIFFIIFFF)V",
                    remap = true
            ),
            remap = false
    )
    private void addSodiumLitVertex(
            VertexConsumer vertexConsumer,
            float x,
            float y,
            float z,
            int color,
            float u,
            float v,
            int overlay,
            int light,
            float normalX,
            float normalY,
            float normalZ,
            Operation<Void> original,
            @Local(argsOnly = true) PoseStack.Pose pose,
            @Local ModelPart.Polygon polygon,
            @Local ModelPart.Vertex vertex
    ) {
        ImmediateBlockEntityLight.RenderContext context = ImmediateBlockEntityLight.active();
        if (context != null) {
            ImmediateBlockEntityLight.VertexLight vertexLight = context.calculate(
                    pose,
                    polygon,
                    vertex,
                    color,
                    light
            );
            color = vertexLight.color();
            light = vertexLight.light();
            normalX = 0.0F;
            normalY = 1.0F;
            normalZ = 0.0F;
        }

        original.call(vertexConsumer, x, y, z, color, u, v, overlay, light, normalX, normalY, normalZ);
    }
}
