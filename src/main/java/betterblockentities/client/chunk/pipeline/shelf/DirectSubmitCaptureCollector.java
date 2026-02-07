package betterblockentities.client.chunk.pipeline.shelf;

/* local */
import betterblockentities.client.model.geometry.RecordingVertexConsumer;

/* mojang */
import com.mojang.blaze3d.vertex.PoseStack;

/* minecraft */
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.item.ItemDisplayContext;

/* annotations */
import org.jspecify.annotations.NonNull;

/* java */
import java.util.List;

/**
 * Direct submit-time capture collector.
 * Extends SubmitNodeStorage so we inherit the full collector implementation,
 * but override the submit kinds we care about so they are converted to shelf
 * capture geometry immediately instead of being stored and replayed later.
 */
final class DirectSubmitCaptureCollector extends SubmitNodeStorage {

    private static final int WHITE_TINT = 0xFFFFFFFF;

    private final RenderTypeClassifier rt;
    private final PackedQuadUtil quadUtil;
    private final RecordedGeometryEmitter emitter;
    private final GeometryBaker.Sink sink;

    DirectSubmitCaptureCollector(
            RenderTypeClassifier rt,
            PackedQuadUtil quadUtil,
            GeometryBaker.Sink sink
    ) {
        this.rt = rt;
        this.quadUtil = quadUtil;
        this.emitter = new RecordedGeometryEmitter(quadUtil);
        this.sink = sink;
    }

    private static PoseStack copyPose(PoseStack source) {
        PoseStack ps = new PoseStack();
        PackedQuadUtil.resetPoseToIdentity(ps);
        ps.last().set(source.last());
        return ps;
    }

    private void emitRecorded(
            RenderType renderType,
            RecordingVertexConsumer rec,
            TextureAtlasSprite fallbackSprite,
            int tintColor
    ) {
        rec.flush();
        emitter.emitRecordedQuads(
                renderType,
                rec.quads(),
                fallbackSprite,
                tintColor,
                sink
        );
    }

    private RecordingVertexConsumer newRecorderWithSprite(TextureAtlasSprite sprite) {
        RecordingVertexConsumer rec = new RecordingVertexConsumer();
        rec.setActiveSprite(sprite);
        return rec;
    }

    @Override public <S> void submitModel(
            @NonNull Model<? super S> model,
            S renderState,
            @NonNull PoseStack poseStack,
            @NonNull RenderType renderType,
            int packedLight,
            int packedOverlay,
            int tintColor,
            TextureAtlasSprite sprite,
            int outlineColor,
            ModelFeatureRenderer.CrumblingOverlay crumblingOverlay
    ) {
        TextureAtlasSprite inferred = null;
        if (sprite == null) {
            var tex = rt.tryExtractTextureId(renderType);
            inferred = tex != null ? quadUtil.tryResolveEntitySprite(tex) : null;
        }

        TextureAtlasSprite fallbackSprite =
                sprite != null ? sprite : (inferred != null ? inferred : quadUtil.missingNoOrNull());
        PoseStack ps = copyPose(poseStack);

        // Warmup pass is intentional.
        // Some special-model paths do not produce stable captured output unless the
        // actual recorded pass is kept separate from an initial render/setup pass.
        {
            RecordingVertexConsumer warmup = newRecorderWithSprite(sprite == null ? inferred : null);

            model.setupAnim(renderState);

            if (sprite != null) {
                var out = sprite.wrap(warmup);
                model.renderToBuffer(ps, out, packedLight, packedOverlay, tintColor);
            } else {
                model.renderToBuffer(ps, warmup, packedLight, packedOverlay, tintColor);
            }

            warmup.flush();
        }

        RecordingVertexConsumer rec = newRecorderWithSprite(sprite == null ? inferred : null);

        model.setupAnim(renderState);

        if (sprite != null) {
            var out = sprite.wrap(rec);
            model.renderToBuffer(ps, out, packedLight, packedOverlay, tintColor);
        } else {
            model.renderToBuffer(ps, rec, packedLight, packedOverlay, tintColor);
        }

        emitRecorded(renderType, rec, fallbackSprite, tintColor);
    }

    @Override public void submitModelPart(
            ModelPart modelPart,
            @NonNull PoseStack poseStack,
            @NonNull RenderType renderType,
            int packedLight,
            int packedOverlay,
            TextureAtlasSprite sprite,
            boolean useItemGlintOverEntityGlint,
            boolean renderGlint,
            int tintColor,
            ModelFeatureRenderer.CrumblingOverlay crumblingOverlay,
            int outlineColor
    ) {
        RecordingVertexConsumer rec = newRecorderWithSprite(sprite);
        PoseStack ps = copyPose(poseStack);

        modelPart.render(ps, rec, packedLight, packedOverlay, tintColor);

        TextureAtlasSprite fallbackSprite =
                sprite != null ? sprite : quadUtil.missingNoOrNull();

        emitRecorded(renderType, rec, fallbackSprite, tintColor);
    }

    @Override public void submitItem(
            PoseStack poseStack,
            @NonNull ItemDisplayContext itemDisplayContext,
            int packedLight,
            int packedOverlay,
            int outlineColor,
            int @NonNull [] tintLayers,
            List<BakedQuad> quads,
            @NonNull RenderType renderType,
            ItemStackRenderState.@NonNull FoilType foilType
    ) {
        var mat = poseStack.last().pose();

        for (BakedQuad quad : quads) {
            sink.accept(
                    PackedQuadUtil.transformQuadToPacked(quad, mat),
                    renderType,
                    tintLayers
            );
        }
    }

    @Override public void submitBlockModel(
            @NonNull PoseStack poseStack,
            @NonNull RenderType renderType,
            @NonNull BlockStateModel blockStateModel,
            float red,
            float green,
            float blue,
            int packedLight,
            int packedOverlay,
            int outlineColor
    ) {
        RecordingVertexConsumer rec = newRecorderWithSprite(null);

        PoseStack ps = copyPose(poseStack);

        ModelBlockRenderer.renderModel(
                ps.last(),
                rec,
                blockStateModel,
                red,
                green,
                blue,
                packedLight,
                packedOverlay
        );

        emitRecorded(renderType, rec, quadUtil.missingNoOrNull(), WHITE_TINT);
    }

    @Override public void submitCustomGeometry(
            PoseStack poseStack,
            @NonNull RenderType renderType,
            SubmitNodeCollector.CustomGeometryRenderer customGeometryRenderer
    ) {
        RecordingVertexConsumer rec = newRecorderWithSprite(null);

        customGeometryRenderer.render(poseStack.last(), rec);

        emitRecorded(renderType, rec, quadUtil.missingNoOrNull(), WHITE_TINT);
    }
}
