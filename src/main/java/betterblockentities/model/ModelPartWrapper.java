package betterblockentities.model;

/* local */
import betterblockentities.mixin.minecraft.ModelPartAccessor;
import betterblockentities.mixin.minecraft.ModelPartPolygonAccessor;
import betterblockentities.mixin.minecraft.ModelPartVertexAccessor;

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

public class ModelPartWrapper {
    /* takes a single ModelPart (with no children) and spits out a bunch of quads */
    public static void toBakedQuads(ModelPart part, List<BakedQuad> output, TextureAtlasSprite sprite) {
        ModelPartAccessor modelAcc = (ModelPartAccessor)(Object)part;
        for (ModelPart.Cube cube : modelAcc.getCubes()) {
            for (ModelPart.Polygon polygon : cube.polygons) {
                /* skip non-quad polygons */
                if (polygon.vertices().length != 4) {
                    continue;
                }

                ModelPartPolygonAccessor polyAcc = (ModelPartPolygonAccessor)(Object)polygon;

                /* polygon vertices */
                ModelPart.Vertex[] vertices = polyAcc.getVertices();

                /* four vertex positions */
                Vector3f[] bakedQuadVertices = new Vector3f[vertices.length];

                /* four packed uvs, one for each vertex */
                long[] packedUvs = new long[vertices.length];

                /* convert polygon normal to a direction */
                Direction direction = normalToDirection(polyAcc.getNormal());

                for (int i = 0; i < polyAcc.getVertices().length; i++) {
                    ModelPart.Vertex vertex = vertices[i];
                    ModelPartVertexAccessor vertexAcc = (ModelPartVertexAccessor)(Object)vertex;

                    /* construct baked quad vertex */
                    Vector3f bakedQuadvertex = new Vector3f(vertex.worldX(), vertex.worldY(), vertex.worldZ()); //normalized to [0,1] block space
                    bakedQuadVertices[i] = bakedQuadvertex;

                    /* pack vertex uvs */
                    //packedUvs[i] = UVPair.pack(vertexAcc.getU(), vertexAcc.getV());
                    float u = sprite.getU(vertexAcc.getU());
                    float v = sprite.getV(vertexAcc.getV());
                    packedUvs[i] = UVPair.pack(u, v);
                }
                BakedQuad baked = new BakedQuad(
                        bakedQuadVertices[0], bakedQuadVertices[1], bakedQuadVertices[2], bakedQuadVertices[3],
                        packedUvs[0], packedUvs[1], packedUvs[2], packedUvs[3],
                        -1, //tint index
                        direction, //face direction
                        sprite, //sprite
                        true, //shade
                        0 //light emission
                );
                output.add(baked);
            }
        }
    }

    /*
        takes in a model part (could be the whole root or children in the root part for example), and spits out quads
        this respects pose stack transforms so this impl will most likely be used the most when converting "BER" geometry
    */
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

    public static void toBakedQuadsVanilla(ModelPart part, List<BakedQuad> output, TextureAtlasSprite sprite, PoseStack stack) {
        ModelPartAccessor modelAcc = (ModelPartAccessor)(Object)part;

        stack.pushPose();

        //modelAcc.translateAndRotateInvoke(stack);

        float modelX = modelAcc.getX();
        float modelY = modelAcc.getY();
        float modelZ = modelAcc.getZ();

        float nModelX = modelX / 16;
        float nModelY = modelY / 16;
        float nModelZ = modelZ / 16;

        float modelXRot = modelAcc.getXRot();
        float modelYRot = modelAcc.getYRot();
        float modelZRot = modelAcc.getZRot();

        float modelXScale = modelAcc.getXScale();
        float modelYScale = modelAcc.getYScale();
        float modelZScale = modelAcc.getZScale();

        stack.translate(nModelX, nModelY, nModelZ);
        if (modelXRot != 0.0F || modelYRot != 0.0F || modelZRot != 0.0F) {
            stack.mulPose((new Quaternionf()).rotationZYX(modelZRot, modelYRot, modelXRot));
        }
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

    /*
        we should be able to decide direction by comparing the passed normal against the predefined normal vectors
        inside the Direction enum else fallback to the axis with the largest component
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
