package betterblockentities.mixin.model;

import net.minecraft.client.model.geom.ModelPart;
import org.joml.Vector3fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ModelPart.Polygon.class)
public interface ModelPartPolygonAccessor {
    @Accessor("vertices")
    ModelPart.Vertex[] getVertices();

    @Accessor("normal")
    Vector3fc getNormal();
}
