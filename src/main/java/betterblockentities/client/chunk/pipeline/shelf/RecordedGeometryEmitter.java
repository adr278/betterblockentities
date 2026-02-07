package betterblockentities.client.chunk.pipeline.shelf;

/* local */
import betterblockentities.client.model.geometry.RecordingVertexConsumer;

/* minecraft */
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;

/* java/misc */
import java.util.List;

final class RecordedGeometryEmitter {
    private final PackedQuadUtil quadUtil;
    private final ThreadLocal<int[]> tlOneTint = ThreadLocal.withInitial(() -> new int[1]);

    RecordedGeometryEmitter(PackedQuadUtil quadUtil) {
        this.quadUtil = quadUtil;
    }

    void emitRecordedAsQuads(
            RenderType rtObj,
            List<RecordingVertexConsumer.Vtx> verts,
            TextureAtlasSprite sprite,
            int tintedColor,
            GeometryBaker.Sink sink
    ) {
        if (verts.isEmpty()) {
            return;
        }
        int[] tintLayers = tlOneTint.get();
        tintLayers[0] = tintedColor;

        int n = verts.size();
        if ((n & 3) != 0) {
            emitRecordedAsTris(rtObj, verts, sink);
            return;
        }
        for (int i = 0; i + 3 < n; i += 4) {
            RecordingVertexConsumer.Vtx a = verts.get(i);
            RecordingVertexConsumer.Vtx b = verts.get(i + 1);
            RecordingVertexConsumer.Vtx c = verts.get(i + 2);
            RecordingVertexConsumer.Vtx d = verts.get(i + 3);

            Direction dir = guessDirection(a, b, c);
            GeometryBaker.PackedQuad q = makePackedQuad(a, b, c, d, dir, sprite, 0);
            sink.accept(q, rtObj, tintLayers);
        }
    }

    void emitRecordedAsTris(
            RenderType rtObj,
            List<RecordingVertexConsumer.Vtx> verts,
            GeometryBaker.Sink sink
    ) {
        if (verts.isEmpty()) {
            return;
        }
        TextureAtlasSprite sprite = quadUtil.missingNoOrNull();
        int n = verts.size();
        for (int i = 0; i + 2 < n; i += 3) {
            RecordingVertexConsumer.Vtx a = verts.get(i);
            RecordingVertexConsumer.Vtx b = verts.get(i + 1);
            RecordingVertexConsumer.Vtx c = verts.get(i + 2);

            Direction dir = guessDirection(a, b, c);
            GeometryBaker.PackedQuad q = makePackedQuad(a, b, c, c, dir, sprite, -1);
            sink.accept(q, rtObj, null);
        }
    }

    private static GeometryBaker.PackedQuad makePackedQuad(
            RecordingVertexConsumer.Vtx a,
            RecordingVertexConsumer.Vtx b,
            RecordingVertexConsumer.Vtx c,
            RecordingVertexConsumer.Vtx d,
            Direction dir,
            TextureAtlasSprite sprite,
            int tintIndex
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
                tintIndex,
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

        if (anx >= any && anx >= anz) return nx >= 0 ? Direction.EAST : Direction.WEST;
        if (any >= anx && any >= anz) return ny >= 0 ? Direction.UP : Direction.DOWN;
        return nz >= 0 ? Direction.SOUTH : Direction.NORTH;
    }
}
