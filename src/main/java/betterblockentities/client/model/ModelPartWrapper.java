package betterblockentities.client.model;

/* local */
import betterblockentities.mixin.model.ModelPartAccessor;
import betterblockentities.mixin.model.ModelPartPolygonAccessor;
import betterblockentities.mixin.model.ModelPartVertexAccessor;

/* minecraft */
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;

/* mojang */
import com.mojang.blaze3d.vertex.PoseStack;

/* java/misc */
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import java.util.List;


/**
 * A utility class for converting an entire Model root tree or a child ModelPart into a single list of BakedQuads
 * Each implementation respects PoseStack transforms and these should be applied before calling.
 */
public class ModelPartWrapper {
    public static void toBakedQuadsWithTransforms(ModelPart part, List<BakedQuad> output, TextureAtlasSprite sprite, PoseStack stack) {
        part.visit(stack, (pose, name, idx, cube) -> {
            for (ModelPart.Polygon poly : cube.polygons) {
                /* skip non-quad polygons */
                if (poly.vertices().length != 4) continue;

                Vector3f[] positions = new Vector3f[4];
                long[] packedUvs = new long[4];

                /* convert polygon normal to face direction */
                Direction dir = normalToDirection(poly.normal());

                /* pack and transform UVS and vertex positions */
                for (int i = 0; i < 4; i++) {
                    ModelPart.Vertex vertex = poly.vertices()[i];
                    Vector3f vec = pose.pose().transformPosition(vertex.worldX(), vertex.worldY(), vertex.worldZ(), new Vector3f());
                    positions[i] = vec;

                    float u = sprite.getU(vertex.u());
                    float v = sprite.getV(vertex.v());
                    packedUvs[i] = UVPair.pack(u, v);
                }

                /* assemble quad */
                BakedQuad baked = new BakedQuad(
                        positions[0], positions[1], positions[2], positions[3],
                        packedUvs[0], packedUvs[1], packedUvs[2], packedUvs[3],
                        -1, //tint
                        dir, //face direction
                        sprite, //sprite
                        true, //shade
                        0 //light emission
                );
                output.add(baked);
            }
        });
    }

    /**
     * skips applying (xRot, yRot, zRot) axis rotation, this seems to mess up geometry coming from mods like EMF (we shouldn't need it anyway)
     */
    public static void toBakedQuadsVanilla(ModelPart part, List<BakedQuad> output, TextureAtlasSprite sprite, PoseStack stack) {
        ModelPartAccessor modelAcc = (ModelPartAccessor)(Object)part;
        stack.pushPose();
        float modelX = modelAcc.getX();
        float modelY = modelAcc.getY();
        float modelZ = modelAcc.getZ();

        float nModelX = modelX / 16;
        float nModelY = modelY / 16;
        float nModelZ = modelZ / 16;

        float modelXScale = modelAcc.getXScale();
        float modelYScale = modelAcc.getYScale();
        float modelZScale = modelAcc.getZScale();

        stack.translate(nModelX, nModelY, nModelZ);
        if (modelXScale != 1.0F || modelYScale != 1.0F || modelZScale != 1.0F) {
            stack.scale(modelXScale, modelYScale, modelZScale);
        }

        PoseStack.Pose pose = stack.last();
        for(ModelPart.Cube cube : modelAcc.getCubes()) {
            for (ModelPart.Polygon poly : cube.polygons) {
                /* skip non-quad polygons */
                if (poly.vertices().length != 4) continue;

                Vector3f[] positions = new Vector3f[4];
                long[] packedUvs = new long[4];

                /* convert polygon normal to face direction */
                Direction dir = normalToDirection(poly.normal());

                /* pack and transform UVS and vertex positions */
                for (int i = 0; i < 4; i++) {
                    /* block/model space - unnormalized */
                    ModelPart.Vertex vertex = poly.vertices()[i];

                    /* world space - normalized */
                    Vector3f vec = pose.pose().transformPosition(vertex.worldX(), vertex.worldY(), vertex.worldZ(), new Vector3f());
                    positions[i] = vec;

                    float u = sprite.getU(vertex.u());
                    float v = sprite.getV(vertex.v());
                    packedUvs[i] = UVPair.pack(u, v);
                }

                /* assemble quad */
                BakedQuad baked = new BakedQuad(
                        positions[0], positions[1], positions[2], positions[3],
                        packedUvs[0], packedUvs[1], packedUvs[2], packedUvs[3],
                        -1, //tint
                        dir, //face direction
                        sprite, //sprite
                        true, //shade
                        0 //light emission
                );
                output.add(baked);
            }
        }

        /* recursion - traverse all children */
        for(ModelPart modelPart : modelAcc.getChildren().values()) {
            toBakedQuadsVanilla(modelPart, output, sprite, stack);
        }
        stack.popPose();
    }

    /**
     * Takes in a normal and outputs a Direction by comparing the normal components
     * in each cardinal direction, else we fall back to the axis with the largest component
     * @param normal Normal (x, y, z)
     * @return Direction
     */
    public static Direction normalToDirection(Vector3fc normal) {
        for (Direction dir : Direction.values()) {
            if (dir.getStepX() == normal.x() &&
                dir.getStepY() == normal.y() &&
                dir.getStepZ() == normal.z()) {
                return dir;
            }
        }

        /* fallback */
        float x = normal.x();
        float y = normal.y();
        float z = normal.z();

        float absX = Math.abs(x);
        float absY = Math.abs(y);
        float absZ = Math.abs(z);

        if (absX > absY && absX > absZ) {
            return x > 0 ? Direction.EAST : Direction.WEST;
        } else if (absY > absZ) {
            return y > 0 ? Direction.UP : Direction.DOWN;
        } else {
            return z > 0 ? Direction.SOUTH : Direction.NORTH;
        }
    }
}
