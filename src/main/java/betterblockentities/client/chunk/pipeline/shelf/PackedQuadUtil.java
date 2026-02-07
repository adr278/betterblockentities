package betterblockentities.client.chunk.pipeline.shelf;

/* minecraft */
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.Identifier;

/* java/misc */
import org.joml.Matrix4f;
import org.joml.Vector3f;

final class PackedQuadUtil {
    private final SpriteRemapper sprites;

    private static final ThreadLocal<V4> TL_V4 = ThreadLocal.withInitial(V4::new);

    private static final class V4 {
        final Vector3f a = new Vector3f();
        final Vector3f b = new Vector3f();
        final Vector3f c = new Vector3f();
        final Vector3f d = new Vector3f();
    }

    PackedQuadUtil(SpriteRemapper sprites) {
        this.sprites = sprites;
    }

    GeometryBaker.PackedQuad normalizeForCaching(GeometryBaker.PackedQuad quad) {
        TextureAtlasSprite src = quad.sprite();
        if (!sprites.isNotBlockAtlas(src)) {
            return quad;
        }
        TextureAtlasSprite dst = sprites.tryGetBlockItemSprite(src);
        if (dst == null) {
            dst = sprites.missingNoOrNull();
        }
        if (dst == src) {
            return quad;
        }
        return remapPackedQuadToSprite(quad, src, dst);
    }

    static void resetPoseToIdentity(PoseStack ps) {
        ps.last().pose().identity();
        ps.last().normal().identity();
    }

    static GeometryBaker.PackedQuad transformQuadToPacked(BakedQuad q, Matrix4f m) {
        V4 v = TL_V4.get();

        Vector3f p0 = v.a.set(q.position0().x(), q.position0().y(), q.position0().z());
        Vector3f p1 = v.b.set(q.position1().x(), q.position1().y(), q.position1().z());
        Vector3f p2 = v.c.set(q.position2().x(), q.position2().y(), q.position2().z());
        Vector3f p3 = v.d.set(q.position3().x(), q.position3().y(), q.position3().z());

        m.transformPosition(p0);
        m.transformPosition(p1);
        m.transformPosition(p2);
        m.transformPosition(p3);

        return new GeometryBaker.PackedQuad(
                p0.x, p0.y, p0.z,
                p1.x, p1.y, p1.z,
                p2.x, p2.y, p2.z,
                p3.x, p3.y, p3.z,
                q.packedUV0(),
                q.packedUV1(),
                q.packedUV2(),
                q.packedUV3(),
                q.direction(),
                q.shade(),
                q.lightEmission(),
                q.tintIndex(),
                q.sprite(),
                null
        );
    }

    TextureAtlasSprite missingNoOrNull() {
        return sprites.missingNoOrNull();
    }

    TextureAtlasSprite tryResolveEntitySprite(Object textureId) {
        return sprites.tryResolveEntitySprite((Identifier) textureId);
    }

    private static GeometryBaker.PackedQuad remapPackedQuadToSprite(
            GeometryBaker.PackedQuad q,
            TextureAtlasSprite src,
            TextureAtlasSprite dst
    ) {
        long uv0 = remapPackedUv(q.uv0(), src, dst);
        long uv1 = remapPackedUv(q.uv1(), src, dst);
        long uv2 = remapPackedUv(q.uv2(), src, dst);
        long uv3 = remapPackedUv(q.uv3(), src, dst);

        return new GeometryBaker.PackedQuad(
                q.x0(), q.y0(), q.z0(),
                q.x1(), q.y1(), q.z1(),
                q.x2(), q.y2(), q.z2(),
                q.x3(), q.y3(), q.z3(),
                uv0, uv1, uv2, uv3,
                q.dir(),
                q.shade(),
                q.lightEmission(),
                q.tintIndex(),
                dst,
                dst
        );
    }

    private static long remapPackedUv(long packed, TextureAtlasSprite src, TextureAtlasSprite dst) {
        float u = UVPair.unpackU(packed);
        float v = UVPair.unpackV(packed);

        float su0 = src.getU0();
        float su1 = src.getU1();
        float sv0 = src.getV0();
        float sv1 = src.getV1();

        float du0 = dst.getU0();
        float du1 = dst.getU1();
        float dv0 = dst.getV0();
        float dv1 = dst.getV1();

        float ru = (su1 != su0) ? (u - su0) / (su1 - su0) : 0.0f;
        float rv = (sv1 != sv0) ? (v - sv0) / (sv1 - sv0) : 0.0f;

        float nu = du0 + ru * (du1 - du0);
        float nv = dv0 + rv * (dv1 - dv0);

        return UVPair.pack(nu, nv);
    }
}
