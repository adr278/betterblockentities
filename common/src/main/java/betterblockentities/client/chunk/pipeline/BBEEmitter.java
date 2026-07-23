package betterblockentities.client.chunk.pipeline;

/* local */
import betterblockentities.mixin.sodium.pipeline.MutableQuadViewImplAccessor;

/* fabric */
import net.fabricmc.fabric.api.renderer.v1.material.RenderMaterial;

/* minecraft */
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.util.RandomSource;

/* sodium */
import net.caffeinemc.mods.sodium.client.render.frapi.mesh.MutableQuadViewImpl;

/* java/misc */
import org.joml.Matrix4f;
import org.joml.Vector3f;
import java.util.List;
import java.util.function.Supplier;

public final class BBEEmitter {
    public static final int NO_QUAD_SPLITTING = 1 << 0; //0b0001 = 1
    public static final int IMMEDIATE_SHADING = 1 << 1; //0b0010 = 2
    public static final int SUPPORTED_FLAGS = NO_QUAD_SPLITTING | IMMEDIATE_SHADING;
    public static final int QUAD_VERTICES = 4;

    private final Vector3f scratch = new Vector3f();
    private MutableQuadViewImpl emitter;
    private RenderMaterial material;
    private TextureAtlasSprite sprite;
    private Matrix4f transform;
    private int color = 0xFFFFFFFF;
    private int flags;

    public void bind(final MutableQuadViewImpl emitter) {
        this.emitter = emitter;
    }

    public void setMaterial(final RenderMaterial material) {
        this.material = material;
    }

    public void setSprite(TextureAtlasSprite sprite) {
        if (sprite != null) {
            this.sprite = sprite;
        }
    }

    public void setTransform(final Matrix4f transform) {
        this.transform = transform;
    }

    public void setColor(final int color) {
        this.color = color;
    }

    public void setFlag(final int flag) {
        if ((flag & ~SUPPORTED_FLAGS) != 0) {
            throw new IllegalArgumentException("Unsupported emitter flag: " + flag);
        }
        this.flags |= flag;
    }

    public void clearFlag(final int flag) {
        if ((flag & ~SUPPORTED_FLAGS) != 0) {
            throw new IllegalArgumentException("Unsupported emitter flag: " + flag);
        }
        this.flags &= ~flag;
    }

    public void emit(List<BakedModel> models, Supplier<RandomSource> randomSupplier) {
        if (this.emitter == null || this.material == null || this.sprite == null) {
            return;
        }

        for (BakedModel model : models) {
            List<BakedQuad> quads = model.getQuads(null, null, randomSupplier.get());

            for (BakedQuad quad : quads) {
                int[] vertices = quad.getVertices();
                ((MutableQuadViewImplAccessor)this.emitter).fromVanillaInternalInvoke(vertices, 0);

                applyMaterial();
                applySprite();
                applyRotation();
                applyColor();
                applyFlags();

                this.emitter.faceNormal();
                this.emitter.emitDirectly();
            }
        }
    }

    private void applyMaterial() {
        this.emitter.material(this.material);
    }

    private void applySprite() {
        final float uMin = this.sprite.getU0();
        final float uSpan = this.sprite.getU1() - uMin;
        final float vMin = this.sprite.getV0();
        final float vSpan = this.sprite.getV1() - vMin;

        for (int i = 0; i < 4; i++) {
            this.emitter.uv(i, uMin + this.emitter.u(i) * uSpan, vMin + this.emitter.v(i) * vSpan);
        }

        this.emitter.cachedSprite(sprite);
    }

    private void applyRotation() {
        for (int i = 0; i < 4; i++) {
            float x = this.emitter.x(i);
            float y = this.emitter.y(i);
            float z = this.emitter.z(i);

            if (this.transform != null) {
                this.transform.transformPosition(x, y, z, this.scratch);
                x = this.scratch.x();
                y = this.scratch.y();
                z = this.scratch.z();
            }

            this.emitter.pos(i, x, y, z);
        }
    }

    private void applyColor() {
        for (int i = 0; i < 4; i++) {
            this.emitter.color(i, this.color);
        }
    }

    private void applyFlags() {
        this.emitter.tag(this.flags);
    }

    public void clearState() {
        this.material = null;
        this.sprite = null;
        this.transform = null;
        this.color = 0xFFFFFFFF;
        this.flags = 0;
    }
}
