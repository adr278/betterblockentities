package betterblockentities.mixin.accessors;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.List;
import java.util.Map;

@Mixin(ModelPart.class)
public interface ModelPartAccessor {
    @Accessor("cubes")
    List<ModelPart.Cube> bbe$getCubes();

    @Accessor("children")
    Map<String, ModelPart> bbe$getChildren();

    @Invoker("visit")
    void bbe$visit(PoseStack poseStack, ModelPart.Visitor visitor);

    @Invoker("translateAndRotate")
    void bbe$translateAndRotate(PoseStack poseStack);

    @Accessor("x")
    float bbe$getX();

    @Accessor("y")
    float bbe$getY();

    @Accessor("z")
    float bbe$getZ();

    @Accessor("xRot")
    float bbe$getXRot();

    @Accessor("yRot")
    float bbe$getYRot();

    @Accessor("zRot")
    float bbe$getZRot();

    @Accessor("xScale")
    float bbe$getXScale();

    @Accessor("yScale")
    float bbe$getYScale();

    @Accessor("zScale")
    float bbe$getZScale();
}
