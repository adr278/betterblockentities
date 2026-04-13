package betterblockentities.client.chunk.pipeline.itemframe;

/* local */
import betterblockentities.client.chunk.pipeline.shelf.GeometryBaker;

/* mojang */
import com.mojang.blaze3d.vertex.PoseStack;

/* minecraft */
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.core.Direction;
import net.minecraft.util.TriState;

/* sodium */
import net.caffeinemc.mods.sodium.client.render.model.MutableQuadViewImpl;

/* joml */
import org.joml.Matrix4f;
import org.joml.Vector3f;

public final class SodiumChunkQuadEmitter {
    private SodiumChunkQuadEmitter() {}

    public static void emitPackedQuad(
            MutableQuadViewImpl emitter,
            PoseStack.Pose pose,
            GeometryBaker.PackedQuad quad,
            int color,
            int light,
            ChunkSectionLayer layer
    ) {
        TextureAtlasSprite sprite = quad.spriteForCacheOrNull() != null ? quad.spriteForCacheOrNull() : quad.sprite();
        if (sprite == null) return;

        Direction dir = quad.dir();
        emitQuad(
                emitter,
                pose,
                layer,
                sprite,
                quad.shade(),
                color,
                light,
                new float[]{
                        quad.x0(), quad.y0(), quad.z0(),
                        quad.x1(), quad.y1(), quad.z1(),
                        quad.x2(), quad.y2(), quad.z2(),
                        quad.x3(), quad.y3(), quad.z3()
                },
                new float[]{
                        UVPair.unpackU(quad.uv0()), UVPair.unpackV(quad.uv0()),
                        UVPair.unpackU(quad.uv1()), UVPair.unpackV(quad.uv1()),
                        UVPair.unpackU(quad.uv2()), UVPair.unpackV(quad.uv2()),
                        UVPair.unpackU(quad.uv3()), UVPair.unpackV(quad.uv3())
                },
                dir.getStepX(),
                dir.getStepY(),
                dir.getStepZ()
        );
    }

    private static void emitQuad(
            MutableQuadViewImpl emitter,
            PoseStack.Pose pose,
            ChunkSectionLayer layer,
            TextureAtlasSprite sprite,
            boolean shade,
            int color,
            int light,
            float[] positions,
            float[] uvs,
            float nx,
            float ny,
            float nz
    ) {
        Matrix4f poseMatrix = pose.pose();
        Vector3f[] transformedPositions = new Vector3f[]{
                new Vector3f(),
                new Vector3f(),
                new Vector3f(),
                new Vector3f()
        };
        Vector3f transformedNormal;
        int resolvedColor = color == ItemFrameSectionAppender.NO_TINT ? -1 : color;

        emitter.clear();
        emitter.cachedSprite(sprite);
        emitter.setRenderType(layer);
        emitter.setCullFace(null);
        emitter.setNominalFace(null);
        emitter.setAmbientOcclusion(TriState.FALSE);
        emitter.setDiffuseShade(shade);
        emitter.setTintIndex(-1);

        for (int vertex = 0; vertex < 4; vertex++) {
            int posIndex = vertex * 3;
            int uvIndex = vertex * 2;

            Vector3f transformedPos = transformedPositions[vertex];
            poseMatrix.transformPosition(
                    positions[posIndex],
                    positions[posIndex + 1],
                    positions[posIndex + 2],
                    transformedPos
            );

            emitter.setPos(vertex, transformedPos.x, transformedPos.y, transformedPos.z);
            emitter.setUV(vertex, uvs[uvIndex], uvs[uvIndex + 1]);
            emitter.setColor(vertex, resolvedColor);
            emitter.setLight(vertex, light);
        }

        transformedNormal = resolveQuadNormal(
                transformedPositions[0],
                transformedPositions[1],
                transformedPositions[2],
                nx,
                ny,
                nz
        );
        for (int vertex = 0; vertex < 4; vertex++) {
            emitter.setNormal(vertex, transformedNormal.x, transformedNormal.y, transformedNormal.z);
        }

        emitter.emitDirectly();
        emitter.clear();
    }

    private static Vector3f resolveQuadNormal(
            Vector3f p0,
            Vector3f p1,
            Vector3f p2,
            float fallbackX,
            float fallbackY,
            float fallbackZ
    ) {
        Vector3f edgeA = new Vector3f(p1).sub(p0);
        Vector3f edgeB = new Vector3f(p2).sub(p0);
        Vector3f normal = edgeA.cross(edgeB);

        Vector3f fallback = new Vector3f(fallbackX, fallbackY, fallbackZ).normalize();
        if (normal.lengthSquared() < 0.00000001f) return fallback;

        normal.normalize();
        if (normal.dot(fallback) < 0.0F) {
            normal.negate();
        }

        return normal;
    }
}
