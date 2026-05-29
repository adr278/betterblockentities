package betterblockentities.client.chunk.pipeline;

/* local */
import betterblockentities.client.model.geometry.GeometryRegistry;

/* fabric */
import net.fabricmc.fabric.api.renderer.v1.material.RenderMaterial;

/* minecraft */
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

/* sodium */
import net.caffeinemc.mods.sodium.client.render.frapi.mesh.MutableQuadViewImpl;

/* java/misc */
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.List;

public final class BBEEmitter {
    private final Vector3f scratch = new Vector3f();
    private final Vector3f normalScratch = new Vector3f();
    private MutableQuadViewImpl emitter;
    private RenderMaterial material;
    private TextureAtlasSprite sprite;
    private Matrix4f transform;
    private int color = 0xFFFFFFFF;
    private boolean disableSplit = false;

    public void bind(final MutableQuadViewImpl emitter) {
        this.emitter = emitter;
    }

    public void setMaterial(final RenderMaterial material) {
        this.material = material;
    }

    public void setSprite(final TextureAtlasSprite sprite) {
        this.sprite = sprite;
    }

    public void setTransform(final Matrix4f transform) {
        this.transform = transform;
    }

    public void setColor(final int color) {
        this.color = color;
    }

    public void setDisableSplit(final boolean disableSplit) {
        this.disableSplit = disableSplit;
    }

    public void emit(final List<GeometryRegistry.QuadTemplate> quads, final int packedLight) {
        if (this.emitter == null || this.material == null || this.sprite == null || quads.isEmpty()) {
            return;
        }

        for (GeometryRegistry.QuadTemplate quad : quads) {
            this.emitter.clear();

            final float[] pos = quad.positions();
            final float[] uv = quad.uvs();
            final float[] normal = quad.normals();
            final float uMin = this.sprite.getU0();
            final float vMin = this.sprite.getV0();
            final float uSpan = this.sprite.getU1() - uMin;
            final float vSpan = this.sprite.getV1() - vMin;
            float normalX = normal[0];
            float normalY = normal[1];
            float normalZ = normal[2];

            if (this.transform != null) {
                this.transform.transformDirection(normalX, normalY, normalZ, this.normalScratch).normalize();
                normalX = this.normalScratch.x();
                normalY = this.normalScratch.y();
                normalZ = this.normalScratch.z();
            }

            for (int i = 0; i < 4; i++) {
                final int posIdx = i * 3;
                final int uvIdx = i * 2;

                float x = pos[posIdx];
                float y = pos[posIdx + 1];
                float z = pos[posIdx + 2];

                if (this.transform != null) {
                    this.transform.transformPosition(x, y, z, this.scratch);
                    x = this.scratch.x();
                    y = this.scratch.y();
                    z = this.scratch.z();
                }

                this.emitter.pos(i, x, y, z);
                this.emitter.color(i, this.color);
                this.emitter.uv(i, uMin + uv[uvIdx] * uSpan, vMin + uv[uvIdx + 1] * vSpan);
                this.emitter.lightmap(i, packedLight);
                this.emitter.normal(i, normalX, normalY, normalZ);
            }

            this.emitter.material(this.material);

            if (this.disableSplit) {
                this.emitter.tag(BBEBlockRenderer.NO_QUAD_SPLITTING_TAG);
            }

            this.emitter.emitDirectly();
        }
    }

    public void clearState() {
        this.material = null;
        this.sprite = null;
        this.transform = null;
        this.color = 0xFFFFFFFF;
        this.disableSplit = false;
    }
}
