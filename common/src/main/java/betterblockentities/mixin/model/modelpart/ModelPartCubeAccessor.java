package betterblockentities.mixin.model.modelpart;

import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ModelPart.Cube.class)
public interface ModelPartCubeAccessor {
    @Accessor("polygons")
    ModelPart.Polygon[] getPolygons();
}
