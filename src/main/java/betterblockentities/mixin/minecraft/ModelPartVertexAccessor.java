package betterblockentities.mixin.minecraft;

import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ModelPart.Vertex.class)
public interface ModelPartVertexAccessor {
    @Accessor("x")
    float getX();

    @Accessor("y")
    float getY();

    @Accessor("z")
    float getZ();

    @Accessor("u")
    float getU();

    @Accessor("v")
    float getV();
}
