package betterblockentities.client.chunk.pipeline.shelf;

/* minecraft */
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.block.model.SimpleModelWrapper;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.QuadCollection;

/* java/misc */
import java.util.ArrayList;
import java.util.List;
import org.joml.Vector3f;

final class LayeredGeometryAssembler {
    static final class Run {
        final ArrayList<GeometryBaker.LayeredPart> out = new ArrayList<>(16);
        ArrayList<BakedQuad> quads = new ArrayList<>(16);
        ChunkSectionLayer layer = null;
        int color = Integer.MIN_VALUE;
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
        run.quads.add(toWrappedBakedQuad(quad));
    }
    GeometryBaker.LayeredPart[] finish(Run run) {
        flush(run);
        return run.out.toArray(new GeometryBaker.LayeredPart[0]);
    }

    private static void flush(Run run) {
        if (run.quads.isEmpty()) {
            return;
        }
        BlockModelPart part = buildPart(run.quads);
        if (part != null) {
            run.out.add(new GeometryBaker.LayeredPart(run.layer, run.color, part));
        }
        run.quads = new ArrayList<>(16);
    }

    private static QuadCollection buildCollection(List<BakedQuad> quads) {
        QuadCollection.Builder builder = new QuadCollection.Builder();
        for (BakedQuad quad : quads) {
            builder.addUnculledFace(quad);
        }
        return builder.build();
    }

    private static TextureAtlasSprite particleFrom(List<BakedQuad> quads) {
        return quads.isEmpty() ? null : quads.getFirst().sprite();
    }

    private static BakedQuad toWrappedBakedQuad(GeometryBaker.PackedQuad q) {
        return new BakedQuad(
                new Vector3f(q.x0(), q.y0(), q.z0()),
                new Vector3f(q.x1(), q.y1(), q.z1()),
                new Vector3f(q.x2(), q.y2(), q.z2()),
                new Vector3f(q.x3(), q.y3(), q.z3()),
                q.uv0(),
                q.uv1(),
                q.uv2(),
                q.uv3(),
                -1,
                q.dir(),
                q.sprite(),
                q.shade(),
                q.lightEmission()
        );
    }

    private static BlockModelPart buildPart(List<BakedQuad> quads) {
        TextureAtlasSprite particle = particleFrom(quads);
        return particle == null ? null : new SimpleModelWrapper(buildCollection(quads), true, particle);
    }
}
