package betterblockentities.client.chunk.pipeline.record;

/* minecraft */
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

/* mixin */
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/* java/misc */
import java.util.ArrayList;
import java.util.List;

/**
 * Records the final vertices written to a VertexConsumer:
 * addVertex(x,y,z) followed by setters. No endVertex().
 * <p>
 * We commit the previous vertex when the next addVertex() begins, and also on flush().
 * <p>
 * Additionally, we can remember an "active sprite" for the batch and optionally remap
 * recorded UVs into atlas UVs for that sprite (SpriteCoordinateExpander-equivalent).
 */
public final class RecordingVertexConsumer implements VertexConsumer {
    public static final class Vtx {
        public float x, y, z;
        public int argb = 0xFFFFFFFF;
        public float u, v;
        public int overlay; // packed (u & 0xFFFF) | (v << 16)
        public int light;   // packed (u & 0xFFFF) | (v << 16)
        public float nx, ny, nz;
    }

    private final ArrayList<Vtx> out = new ArrayList<>();
    private Vtx cur;
    private boolean uvsRemapped;

    /**
     * The stitched atlas sprite that UVs should be expanded into (if non-null).
     * This is meant to be set by the caller when tessellating model/modelpart/custom submits
     * that are actually sampling a non-atlas texture that you stitched into the BLOCK atlas.
     */
    private @Nullable TextureAtlasSprite activeSprite;

    public List<Vtx> vertices() {
        return out;
    }

    public void clear() {
        out.clear();
        cur = null;
        activeSprite = null;
        uvsRemapped = false;
    }

    public void flush() {
        if (cur != null) {
            out.add(cur);
            cur = null;
        }
    }

    public void setActiveSprite(@Nullable TextureAtlasSprite sprite) {
        this.activeSprite = sprite;
    }

    public @Nullable TextureAtlasSprite getActiveSprite() {
        return this.activeSprite;
    }

    /**
     * Remap recorded UVs into atlas UVs for the current active sprite.
     * <p>
     * This is the missing piece when the upstream rendering path used an entity RenderType
     * (raw texture) but you stitched that texture into the BLOCK atlas and want to mesh it.
     * <p>
     * Heuristic:
     * - If U or V is > 1.0, treat it as pixels.
     * - Otherwise treat it as 0..1 and scale by 16 before converting to sprite UV.
     * <p>
     * Call this after you've finished recording (flush) and before emitting quads.
     */
    public void remapUvsToActiveSpriteIfPresent() {
        TextureAtlasSprite s = this.activeSprite;
        if (s == null) return;

        if (uvsRemapped) return;

        if (out.isEmpty() && cur == null) return;

        flush();

        // Sprite atlas UV bounds
        final float u0 = s.getU0();
        final float u1 = s.getU1();
        final float v0 = s.getV0();
        final float v1 = s.getV1();

        // Sprite pixel size (for the "pixel UV" case)
        final int w = s.contents().width();
        final int h = s.contents().height();

        for (Vtx v : out) {
            float uIn = v.u;
            float vIn = v.v;

            float uNorm = (Math.abs(uIn) <= 1.0f) ? uIn : (w > 0 ? (uIn / (float) w) : uIn);
            float vNorm = (Math.abs(vIn) <= 1.0f) ? vIn : (h > 0 ? (vIn / (float) h) : vIn);

            if (!Float.isFinite(uNorm)) uNorm = 0.0f;
            if (!Float.isFinite(vNorm)) vNorm = 0.0f;

            if (uNorm < 0.0f) uNorm = 0.0f;
            if (uNorm > 1.0f) uNorm = 1.0f;
            if (vNorm < 0.0f) vNorm = 0.0f;
            if (vNorm > 1.0f) vNorm = 1.0f;

            // SpriteCoordinateExpander equivalent:
            v.u = u0 + (u1 - u0) * uNorm;
            v.v = v0 + (v1 - v0) * vNorm;
        }
        uvsRemapped = true;
    }

    @Override public @NonNull VertexConsumer addVertex(float x, float y, float z) {
        if (cur != null) out.add(cur);
        cur = new Vtx();
        cur.x = x;
        cur.y = y;
        cur.z = z;
        return this;
    }

    @Override public @NonNull VertexConsumer setColor(int r, int g, int b, int a) {
        int argb = ((a & 255) << 24) | ((r & 255) << 16) | ((g & 255) << 8) | (b & 255);
        return setColor(argb);
    }

    @Override public @NonNull VertexConsumer setColor(int argb) {
        if (cur != null) cur.argb = argb;
        return this;
    }

    @Override public @NonNull VertexConsumer setUv(float u, float v) {
        if (cur != null) {
            cur.u = u;
            cur.v = v;
        }
        return this;
    }

    @Override public @NonNull VertexConsumer setUv1(int u, int v) {
        if (cur != null) cur.overlay = (u & 0xFFFF) | ((v & 0xFFFF) << 16);
        return this;
    }

    @Override public @NonNull VertexConsumer setUv2(int u, int v) {
        if (cur != null) cur.light = (u & 0xFFFF) | ((v & 0xFFFF) << 16);
        return this;
    }

    @Override public @NonNull VertexConsumer setNormal(float nx, float ny, float nz) {
        if (cur != null) {
            cur.nx = nx;
            cur.ny = ny;
            cur.nz = nz;
        }
        return this;
    }

    @Override public @NonNull VertexConsumer setLineWidth(float f) {
        return this;
    }
}
