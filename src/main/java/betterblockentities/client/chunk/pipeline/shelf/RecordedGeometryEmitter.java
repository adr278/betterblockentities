package betterblockentities.client.chunk.pipeline.shelf;

/* local */
import betterblockentities.client.model.geometry.RecordingVertexConsumer;

/* minecraft */
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;

/* java */
import java.util.List;

final class RecordedGeometryEmitter {
    private final PackedQuadUtil quadUtil;
    private final ThreadLocal<int[]> tlOneTint = ThreadLocal.withInitial(() -> new int[1]);

    RecordedGeometryEmitter(PackedQuadUtil quadUtil) {
        this.quadUtil = quadUtil;
    }

    void emitRecordedQuads(
            RenderType rtObj,
            List<RecordingVertexConsumer.RecordedQuad> quads,
            TextureAtlasSprite fallbackSprite,
            int tintedColor,
            GeometryBaker.Sink sink
    ) {
        if (quads.isEmpty()) {
            return;
        }

        int[] tintLayers = tlOneTint.get();
        tintLayers[0] = tintedColor;

        TextureAtlasSprite missing = quadUtil.missingNoOrNull();

        for (RecordingVertexConsumer.RecordedQuad rq : quads) {
            TextureAtlasSprite sprite = rq.sprite != null
                    ? rq.sprite
                    : (fallbackSprite != null ? fallbackSprite : missing);
            Direction dir = guessDirection(rq.a, rq.b, rq.c);

            GeometryBaker.PackedQuad q = makePackedQuad(
                    rq.a,
                    rq.b,
                    rq.c,
                    rq.d,
                    dir,
                    sprite
            );
            sink.accept(q, rtObj, tintLayers);
        }
    }

    private static GeometryBaker.PackedQuad makePackedQuad(
            RecordingVertexConsumer.Vtx a,
            RecordingVertexConsumer.Vtx b,
            RecordingVertexConsumer.Vtx c,
            RecordingVertexConsumer.Vtx d,
            Direction dir,
            TextureAtlasSprite sprite
    ) {
        long uv0 = UVPair.pack(a.u, a.v);
        long uv1 = UVPair.pack(b.u, b.v);
        long uv2 = UVPair.pack(c.u, c.v);
        long uv3 = UVPair.pack(d.u, d.v);

        return new GeometryBaker.PackedQuad(
                a.x, a.y, a.z,
                b.x, b.y, b.z,
                c.x, c.y, c.z,
                d.x, d.y, d.z,
                uv0, uv1, uv2, uv3,
                dir,
                true,
                0,
                0,
                sprite,
                null
        );
    }

    private static Direction guessDirection(
            RecordingVertexConsumer.Vtx a,
            RecordingVertexConsumer.Vtx b,
            RecordingVertexConsumer.Vtx c
    ) {
        float ax = b.x - a.x;
        float ay = b.y - a.y;
        float az = b.z - a.z;

        float bx = c.x - a.x;
        float by = c.y - a.y;
        float bz = c.z - a.z;

        float nx = ay * bz - az * by;
        float ny = az * bx - ax * bz;
        float nz = ax * by - ay * bx;

        float anx = Math.abs(nx);
        float any = Math.abs(ny);
        float anz = Math.abs(nz);

        if (anx >= any && anx >= anz) return nx >= 0.0f ? Direction.EAST : Direction.WEST;
        if (any >= anx && any >= anz) return ny >= 0.0f ? Direction.UP : Direction.DOWN;
        return nz >= 0.0f ? Direction.SOUTH : Direction.NORTH;
    }
}
