package betterblockentities.client.chunk.pipeline.shelf;

/* mojang */
import com.mojang.blaze3d.platform.Transparency;

/* minecraft */
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.SimpleModelWrapper;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.client.resources.model.sprite.Material;

/* java */
import java.util.ArrayList;
import java.util.List;

/* joml */
import org.joml.Vector3f;

final class LayeredGeometryAssembler {
    static final class Run {
        final ArrayList<GeometryBaker.LayeredPart> out = new ArrayList<>(16);
        final ArrayList<BakedQuad> quads = new ArrayList<>(16);
        ChunkSectionLayer layer = null;
        int color = Integer.MIN_VALUE;

        void reset() {
            out.clear();
            quads.clear();
            layer = null;
            color = Integer.MIN_VALUE;
        }
    }

    Run newRun() {
        return new Run();
    }

    void accept(
            Run run,
            GeometryBaker.PackedQuad quad,
            ChunkSectionLayer layer,
            int color
    ) {
        if (run.layer != layer || run.color != color) {
            flush(run);
            run.layer = layer;
            run.color = color;
        }

        run.quads.add(toWrappedBakedQuad(quad, layer));
    }

    GeometryBaker.LayeredPart[] finish(Run run) {
        flush(run);
        GeometryBaker.LayeredPart[] out = run.out.toArray(new GeometryBaker.LayeredPart[0]);
        run.reset();
        return out;
    }

    private static void flush(Run run) {
        if (run.quads.isEmpty()) return;

        BlockStateModelPart part = buildPart(run.quads, run.layer);
        if (part != null) {
            run.out.add(new GeometryBaker.LayeredPart(run.layer, run.color, part));
        }

        run.quads.clear();
    }

    private static QuadCollection buildCollection(List<BakedQuad> quads) {
        QuadCollection.Builder builder = new QuadCollection.Builder();
        for (BakedQuad quad : quads) {
            builder.addUnculledFace(quad);
        }
        return builder.build();
    }

    private static TextureAtlasSprite particleFrom(List<BakedQuad> quads) {
        return quads.isEmpty() ? null : quads.getFirst().materialInfo().sprite();
    }

    private static Transparency transparencyForLayer(ChunkSectionLayer layer) {
        return switch (layer) {
            case SOLID -> Transparency.NONE;
            case CUTOUT -> Transparency.TRANSPARENT;
            case TRANSLUCENT -> Transparency.TRANSLUCENT;
        };
    }

    private static BakedQuad.MaterialInfo materialInfoFor(
            GeometryBaker.PackedQuad q,
            ChunkSectionLayer layer
    ) {
        TextureAtlasSprite sprite = q.sprite();

        Material.Baked bakedMaterial = new Material.Baked(
                sprite,
                layer == ChunkSectionLayer.TRANSLUCENT
        );

        return BakedQuad.MaterialInfo.of(
                bakedMaterial,
                transparencyForLayer(layer),
                q.tintIndex(),
                q.shade(),
                q.lightEmission()
        );
    }

    private static BakedQuad toWrappedBakedQuad(
            GeometryBaker.PackedQuad q,
            ChunkSectionLayer layer
    ) {
        return new BakedQuad(
                new Vector3f(q.x0(), q.y0(), q.z0()),
                new Vector3f(q.x1(), q.y1(), q.z1()),
                new Vector3f(q.x2(), q.y2(), q.z2()),
                new Vector3f(q.x3(), q.y3(), q.z3()),
                q.uv0(),
                q.uv1(),
                q.uv2(),
                q.uv3(),
                q.dir(),
                materialInfoFor(q, layer)
        );
    }

    private static BlockStateModelPart buildPart(
            List<BakedQuad> quads,
            ChunkSectionLayer layer
    ) {
        TextureAtlasSprite particle = particleFrom(quads);
        if (particle == null) return null;

        Material.Baked particleMaterial = new Material.Baked(
                particle,
                layer == ChunkSectionLayer.TRANSLUCENT
        );

        return new SimpleModelWrapper(
                buildCollection(quads),
                false,
                particleMaterial
        );
    }
}
