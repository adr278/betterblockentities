package betterblockentities.client.model.geometry;

/* local */
import betterblockentities.client.BBE;
import betterblockentities.mixin.model.modelpart.ModelPartCubeAccessor;

/* minecraft */
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;

/* mojang */
import com.mojang.blaze3d.vertex.PoseStack;

/* java/misc */
import org.joml.Vector3f;
import org.joml.Vector3fc;
import java.util.List;

/**
 * Utility for baking ModelPart trees into reusable quad templates.
 */
public class ModelUtility {
    private static final Vector3f scratch = new Vector3f();

    public static void toBakedQuads(ModelPart part, List<BakedQuad> output, TextureAtlasSprite sprite, PoseStack stack) {
        part.visit(stack, (pose, name, idx, cube) -> {
            ModelPartCubeAccessor acc = (ModelPartCubeAccessor)cube;
            for (ModelPart.Polygon poly : acc.getPolygons()) {
                if (poly.vertices.length != 4) {
                    BBE.getLogger().error("Non-quad polygon detected when assembling block geometry! Skipping");
                    continue;
                }

                int[] vertices = new int[32];

                Vector3f normal = pose.transformNormal(poly.normal, new Vector3f());

                for (int i = 0; i < 4; i++) {
                    ModelPart.Vertex vertex = poly.vertices[i];

                    Vector3f pos = pose.pose().transformPosition(vertex.pos.x / 16, vertex.pos.y / 16, vertex.pos.z / 16, scratch);

                    int baseIndex = i * 8;

                    //vertex position
                    vertices[baseIndex] = Float.floatToRawIntBits(pos.x());
                    vertices[baseIndex + 1] = Float.floatToRawIntBits(pos.y());
                    vertices[baseIndex + 2] = Float.floatToRawIntBits(pos.z());

                    //color
                    vertices[baseIndex + 3] = -1;

                    //uv
                    vertices[baseIndex + 4] = Float.floatToRawIntBits(vertex.u);
                    vertices[baseIndex + 5] = Float.floatToRawIntBits(vertex.v);

                    //unused it seems like
                    vertices[baseIndex + 6] = 0;
                    vertices[baseIndex + 7] = 0;
                }

                Direction face = normalToDirection(normal);

                output.add(new BakedQuad(
                        vertices,
                        -1,
                        face,
                        sprite,
                        true
                ));
            }
        });
    }

    public static Direction normalToDirection(final Vector3fc normal) {
        for (Direction dir : Direction.values()) {
            if (dir.getStepX() == Math.round(normal.x())
                    && dir.getStepY() == Math.round(normal.y())
                    && dir.getStepZ() == Math.round(normal.z())) {
                return dir;
            }
        }

        final float x = normal.x();
        final float y = normal.y();
        final float z = normal.z();
        final float absX = Math.abs(x);
        final float absY = Math.abs(y);
        final float absZ = Math.abs(z);

        if (absX > absY && absX > absZ) {
            return x > 0 ? Direction.EAST : Direction.WEST;
        }
        if (absY > absZ) {
            return y > 0 ? Direction.UP : Direction.DOWN;
        }
        return z > 0 ? Direction.SOUTH : Direction.NORTH;
    }
}
