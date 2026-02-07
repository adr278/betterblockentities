package betterblockentities.client.model.geometry;

/* mojang */
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;

/* minecraft */
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

/* sodium */
import net.caffeinemc.mods.sodium.api.vertex.buffer.VertexBufferWriter;

/* annotations */
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/* java */
import java.util.ArrayList;
import java.util.List;

/* lwjgl */
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

    public static final class RecordedQuad {
        public Vtx a;
        public Vtx b;
        public Vtx c;
        public Vtx d;
        public @Nullable TextureAtlasSprite sprite;

        void set(Vtx a, Vtx b, Vtx c, Vtx d, @Nullable TextureAtlasSprite sprite) {
            this.a = a;
            this.b = b;
            this.c = c;
            this.d = d;
            this.sprite = sprite;
        }
    }

    /** Pooled vertex storage. Active vertices are [0, vertexCount). */
    private final ArrayList<Vtx> vertices = new ArrayList<>(256);
    private int vertexCount;

    /** Pooled quad storage. Active quads are [0, quadCount). */
    private final ArrayList<RecordedQuad> quads = new ArrayList<>(128);
    private int quadCount;

    /** Currently-being-written vertex. */
    private @Nullable Vtx cur;

    /** Tracks vertices for the current quad assembly. */
    private int quadVertexCount;

    /**
     * Sprite to expand local UVs into atlas UVs.
     * When null, UVs are recorded exactly as submitted.
     */
    private @Nullable TextureAtlasSprite activeSprite;

    /** Returns recorded quads (active prefix only). */
    public List<RecordedQuad> quads() {
        flush();
        return quads.subList(0, quadCount);
    }

    public void clear() {
        vertexCount = 0;
        quadCount = 0;
        quadVertexCount = 0;
        cur = null;
        activeSprite = null;
        quads.clear();
        vertices.clear();
    }

    /**
     * Commits the current vertex, if present.
     * Quads are formed strictly in submission order: every 4 committed vertices.
     */
    public void flush() {
        finishCurrentVertexIfNeeded();
    }

    public void setActiveSprite(@Nullable TextureAtlasSprite sprite) {
        this.activeSprite = sprite;
    }

    private Vtx nextVertex() {
        Vtx v;
        if (vertexCount < vertices.size()) {
            v = vertices.get(vertexCount);
            v.reset();
        } else {
            v = new Vtx();
            vertices.add(v);
        }
        vertexCount++;
        return v;
    }

    private RecordedQuad nextQuad() {
        RecordedQuad q;
        if (quadCount < quads.size()) {
            q = quads.get(quadCount);
        } else {
            q = new RecordedQuad();
            quads.add(q);
        }
        quadCount++;
        return q;
    }

    private void finishCurrentVertexIfNeeded() {
        if (cur == null) {
            return;
        }

        cur = null;
        quadVertexCount++;

        if ((quadVertexCount & 3) == 0) {
            int base = quadVertexCount - 4;
            RecordedQuad q = nextQuad();
            q.set(
                    vertices.get(base),
                    vertices.get(base + 1),
                    vertices.get(base + 2),
                    vertices.get(base + 3),
                    activeSprite
            );
        }
    }

    @Override public @NonNull VertexConsumer addVertex(float x, float y, float z) {
        finishCurrentVertexIfNeeded();

        Vtx v = nextVertex();
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
        if (v != null) {
            v.argb = argb;
        }
        return this;
    }

    @Override public @NonNull VertexConsumer setUv(float u, float v) {
        Vtx vx = cur;
        if (vx != null) {
            TextureAtlasSprite sprite = this.activeSprite;
            if (sprite != null) {
                float mappedU = mapU(sprite, u);
                float mappedV = mapV(sprite, v);
                vx.u = mappedU;
                vx.v = mappedV;
            } else {
                vx.u = u;
                vx.v = v;
            }
        }
        return this;
    }

    @Override public @NonNull VertexConsumer setUv1(int u, int v) {
        Vtx vx = cur;
        if (vx != null) {
            vx.overlay = (u & 0xFFFF) | ((v & 0xFFFF) << 16);
        }
        return this;
    }

    @Override public @NonNull VertexConsumer setUv2(int u, int v) {
        Vtx vx = cur;
        if (vx != null) {
            vx.light = (u & 0xFFFF) | ((v & 0xFFFF) << 16);
        }
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
        finishCurrentVertexIfNeeded();

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

            Vtx v = nextVertex();
            v.x = MemoryUtil.memGetFloat(p);
            v.y = MemoryUtil.memGetFloat(p + 4L);
            v.z = MemoryUtil.memGetFloat(p + 8L);
            v.argb = MemoryUtil.memGetInt(p + 12L);

            float u = MemoryUtil.memGetFloat(p + 16L);
            float vv = MemoryUtil.memGetFloat(p + 20L);

            TextureAtlasSprite sprite = this.activeSprite;
            if (sprite != null) {
                v.u = mapU(sprite, u);
                v.v = mapV(sprite, vv);
            } else {
                v.u = u;
                v.v = vv;
            }

            v.overlay = MemoryUtil.memGetInt(p + 24L);
            v.light = MemoryUtil.memGetInt(p + 28L);

            v.nx = unpackByteNormal(MemoryUtil.memGetByte(p + 32L));
            v.ny = unpackByteNormal(MemoryUtil.memGetByte(p + 33L));
            v.nz = unpackByteNormal(MemoryUtil.memGetByte(p + 34L));

            quadVertexCount++;
            if ((quadVertexCount & 3) == 0) {
                int base = quadVertexCount - 4;
                RecordedQuad q = nextQuad();
                q.set(
                        vertices.get(base),
                        vertices.get(base + 1),
                        vertices.get(base + 2),
                        vertices.get(base + 3),
                        activeSprite
                );
            }
        }
        cur = null;
    }

    private static float mapU(TextureAtlasSprite s, float uIn) {
        float uNorm;
        if (Math.abs(uIn) <= 1.0f) {
            uNorm = uIn;
        } else {
            int w = s.contents().width();
            uNorm = w > 0 ? (uIn / (float) w) : uIn;
        }
        if (!Float.isFinite(uNorm)) {
            uNorm = 0.0f;
        }
        return s.getU0() + (s.getU1() - s.getU0()) * uNorm;
    }

    private static float mapV(TextureAtlasSprite s, float vIn) {
        float vNorm;
        if (Math.abs(vIn) <= 1.0f) {
            vNorm = vIn;
        } else {
            int h = s.contents().height();
            vNorm = h > 0 ? (vIn / (float) h) : vIn;
        }
        if (!Float.isFinite(vNorm)) {
            vNorm = 0.0f;
        }
        return s.getV0() + (s.getV1() - s.getV0()) * vNorm;
    }

    private static float unpackByteNormal(byte value) {
        return Math.max(-1.0f, value / 127.0f);
    }
}
