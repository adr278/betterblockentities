package betterblockentities.client.model.geometry;

/* minecraft */
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

/* sodium */
import net.caffeinemc.mods.sodium.api.vertex.buffer.VertexBufferWriter;

/* mixin */
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/* java/misc */
import java.util.ArrayList;
import java.util.List;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

/**
 * Records the final vertices written to a VertexConsumer:
 * addVertex(x,y,z) followed by setters. No endVertex().
 * <p>
 * We commit the previous vertex when the next addVertex() begins, and also on flush().
 * <p>
 * Additionally, we can remember an "active sprite" for the batch and optionally remap
 * recorded UVs into atlas UVs for that sprite (SpriteCoordinateExpander-equivalent).
 * <p>
 * This class is intended to be reused (e.g. via ThreadLocal).
 * It pools Vtx objects and avoids per-vertex allocations.
 */
public final class RecordingVertexConsumer implements VertexConsumer, VertexBufferWriter {
    public static final class Vtx {
        public float x, y, z;
        public int argb = 0xFFFFFFFF;
        public float u, v;
        public int overlay;
        public int light;
        public float nx, ny, nz;

        // Reset fields that should default per-vertex.
        void reset() {
            argb = 0xFFFFFFFF;
            u = v = 0.0f;
            overlay = 0;
            light = 0;
            nx = ny = nz = 0.0f;
        }
    }

    /** Pooled vertex storage. Active vertices are [0, size). */
    private final ArrayList<Vtx> out = new ArrayList<>(256);
    private int size;

    /** Currently-being-written vertex (not yet committed into [0, size)). */
    private @Nullable Vtx cur;

    private boolean uvsRemapped;

    /**
     * The stitched atlas sprite that UVs should be expanded into (if non-null).
     * This is meant to be set by the caller when tessellating model/modelpart/custom submits
     * that are actually sampling a non-atlas texture that we stitched into the BLOCK atlas.
     */
    private @Nullable TextureAtlasSprite activeSprite;

    /** Returns recorded vertices (active prefix only). This is a view and does not allocate. */
    public List<Vtx> vertices() {
        return out.subList(0, size);
    }

    /** Clears logical contents but keeps pooled vertex objects and backing array capacity. */
    public void clear() {
        // Don't out.clear(); we want to keep pooled Vtx objects and list capacity.
        size = 0;
        cur = null;
        activeSprite = null;
        uvsRemapped = false;
    }

    /** Commits the current vertex (if any) into the active list. */
    public void flush() {
        if (cur != null) {
            // cur is already a pooled instance; just make it part of the active prefix.
            cur = null;
        }
    }

    public void setActiveSprite(@Nullable TextureAtlasSprite sprite) {
        this.activeSprite = sprite;
        this.uvsRemapped = false; // sprite change implies remap state invalid.
    }

    /**
     * Remap recorded UVs into atlas UVs for the current active sprite.
     * <p>
     * Heuristic:
     * - If |U| and |V| are <= 1.0, treat them as normalized 0..1.
     * - Otherwise treat them as "pixel-ish" and normalize by sprite width/height.
     * <p>
     * This intentionally does NOT clamp to [0,1], to preserve tiling/out-of-range UV intent.
     * Non-finite UVs are sanitized to 0.
     * <p>
     * Call this after we've finished recording (flush) and before emitting quads.
     */
    public void remapUvsToActiveSpriteIfPresent() {
        TextureAtlasSprite s = this.activeSprite;
        if (s == null) return;
        if (uvsRemapped) return;
        // Ensure current vertex is included in the active prefix.
        flush();
        if (size == 0) return;
        // Sprite atlas UV bounds.
        final float u0 = s.getU0();
        final float u1 = s.getU1();
        final float v0 = s.getV0();
        final float v1 = s.getV1();
        // Sprite pixel size.
        final int w = s.contents().width();
        final int h = s.contents().height();

        final float du = (u1 - u0);
        final float dv = (v1 - v0);

        for (int i = 0; i < size; i++) {
            Vtx v = out.get(i);

            float uIn = v.u;
            float vIn = v.v;

            float uNorm;
            float vNorm;

            if (Math.abs(uIn) <= 1.0f) {
                uNorm = uIn;
            } else {
                uNorm = (w > 0) ? (uIn / (float) w) : uIn;
            }

            if (Math.abs(vIn) <= 1.0f) {
                vNorm = vIn;
            } else {
                vNorm = (h > 0) ? (vIn / (float) h) : vIn;
            }
            if (!Float.isFinite(uNorm)) uNorm = 0.0f;
            if (!Float.isFinite(vNorm)) vNorm = 0.0f;
            // SpriteCoordinateExpander-equivalent mapping into atlas UV space.
            v.u = u0 + du * uNorm;
            v.v = v0 + dv * vNorm;
        }
        uvsRemapped = true;
    }

    private Vtx nextVtx() {
        if (size < out.size()) {
            Vtx v = out.get(size);
            size++;
            v.reset();
            return v;
        }
        Vtx v = new Vtx();
        out.add(v);
        size++;
        // v.reset() not needed; ctor sets defaults via field initializers.
        return v;
    }

    @Override public @NonNull VertexConsumer addVertex(float x, float y, float z) {
        // Commit previous vertex (if any) simply by dropping cur; it is already in active prefix.
        Vtx v = nextVtx();
        v.x = x;
        v.y = y;
        v.z = z;
        cur = v;
        return this;
    }

    @Override public @NonNull VertexConsumer setColor(int r, int g, int b, int a) {
        int argb = ((a & 255) << 24) | ((r & 255) << 16) | ((g & 255) << 8) | (b & 255);
        return setColor(argb);
    }

    @Override public @NonNull VertexConsumer setColor(int argb) {
        Vtx v = cur;
        if (v != null) v.argb = argb;
        return this;
    }

    @Override public @NonNull VertexConsumer setUv(float u, float v) {
        Vtx vx = cur;
        if (vx != null) {
            vx.u = u;
            vx.v = v;
        }
        return this;
    }

    @Override public @NonNull VertexConsumer setUv1(int u, int v) {
        Vtx vx = cur;
        if (vx != null) vx.overlay = (u & 0xFFFF) | ((v & 0xFFFF) << 16);
        return this;
    }

    @Override public @NonNull VertexConsumer setUv2(int u, int v) {
        Vtx vx = cur;
        if (vx != null) vx.light = (u & 0xFFFF) | ((v & 0xFFFF) << 16);
        return this;
    }

    @Override public @NonNull VertexConsumer setNormal(float nx, float ny, float nz) {
        Vtx v = cur;
        if (v != null) {
            v.nx = nx;
            v.ny = ny;
            v.nz = nz;
        }
        return this;
    }

    @Override public @NonNull VertexConsumer setLineWidth(float f) {
        return this;
    }

    @Override public boolean canUseIntrinsics() {
        return true;
    }

    @Override public void push(MemoryStack stack, long ptr, int count, VertexFormat format) {
        if (count <= 0) {
            return;
        }

        flush();
        if (format == DefaultVertexFormat.NEW_ENTITY) {
            pushNewEntity(ptr, count);
            return;
        }
        throw new IllegalArgumentException(
                "RecordingVertexConsumer only supports Sodium push() for NEW_ENTITY, got " + format
        );
    }

    private void pushNewEntity(long ptr, int count) {
        final int stride = DefaultVertexFormat.NEW_ENTITY.getVertexSize();

        for (int i = 0; i < count; i++) {
            long p = ptr + (long) i * stride;

            Vtx v = nextVtx();

            v.x = MemoryUtil.memGetFloat(p);
            v.y = MemoryUtil.memGetFloat(p + 4L);
            v.z = MemoryUtil.memGetFloat(p + 8L);

            v.argb = MemoryUtil.memGetInt(p + 12L);

            v.u = MemoryUtil.memGetFloat(p + 16L);
            v.v = MemoryUtil.memGetFloat(p + 20L);

            v.overlay = MemoryUtil.memGetInt(p + 24L);
            v.light = MemoryUtil.memGetInt(p + 28L);

            v.nx = unpackByteNormal(MemoryUtil.memGetByte(p + 32L));
            v.ny = unpackByteNormal(MemoryUtil.memGetByte(p + 33L));
            v.nz = unpackByteNormal(MemoryUtil.memGetByte(p + 34L));
        }
        cur = null;
    }

    private static float unpackByteNormal(byte value) {
        return Math.max(-1.0f, value / 127.0f);
    }
}
