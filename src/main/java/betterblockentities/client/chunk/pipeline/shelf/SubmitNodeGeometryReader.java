package betterblockentities.client.chunk.pipeline.shelf;

/* local */
import betterblockentities.client.model.geometry.RecordingVertexConsumer;
import betterblockentities.mixin.render.immediate.blockentity.shelf.CustomFeatureRendererStorageAccessor;
import betterblockentities.mixin.render.immediate.blockentity.shelf.ModelFeatureRendererStorageAccessor;
import betterblockentities.mixin.render.immediate.blockentity.shelf.ModelPartFeatureRendererStorageAccessor;

/* minecraft */
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.CustomFeatureRenderer;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.feature.ModelPartFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

final class SubmitNodeGeometryReader {
    private final RenderTypeClassifier rt;
    private final PackedQuadUtil quadUtil;
    private final RecordedGeometryEmitter emitter;

    SubmitNodeGeometryReader(RenderTypeClassifier rt, PackedQuadUtil quadUtil) {
        this.rt = rt;
        this.quadUtil = quadUtil;
        this.emitter = new RecordedGeometryEmitter(quadUtil);
    }

    void read(SubmitNodeCollection coll, GeometryBaker.Sink sink) {
        readItemSubmits(coll, sink);
        readModelSubmits(coll, sink);
        readModelPartSubmits(coll, sink);
        readCustomGeometrySubmits(coll, sink);
    }

    private void readItemSubmits(SubmitNodeCollection coll, GeometryBaker.Sink sink) {
        for (SubmitNodeStorage.ItemSubmit submit : coll.getItemSubmits()) {
            var mat = submit.pose().pose();
            int[] tints = submit.tintLayers();
            RenderType rtObj = submit.renderType();
            for (var quad : submit.quads()) {
                sink.accept(PackedQuadUtil.transformQuadToPacked(quad, mat), rtObj, tints);
            }
        }
    }

    private void readModelSubmits(SubmitNodeCollection coll, GeometryBaker.Sink sink) {
        ModelFeatureRenderer.Storage storage = coll.getModelSubmits();
        ModelFeatureRendererStorageAccessor acc = (ModelFeatureRendererStorageAccessor) storage;
        for (var e : acc.getOpaqueModelSubmits().entrySet()) {
            RenderType rtObj = e.getKey();
            for (SubmitNodeStorage.ModelSubmit<?> sub : e.getValue()) {
                tessellateModelSubmit(rtObj, sub, sink);
            }
        }
        for (SubmitNodeStorage.TranslucentModelSubmit<?> t : acc.getTranslucentModelSubmits()) {
            tessellateModelSubmit(t.renderType(), t.modelSubmit(), sink);
        }
    }

    private <S> void tessellateModelSubmit(
            RenderType rtObj,
            SubmitNodeStorage.ModelSubmit<S> sub,
            GeometryBaker.Sink sink
    ) {
        RecordingVertexConsumer rec = new RecordingVertexConsumer();

        PoseStack ps = new PoseStack();
        PackedQuadUtil.resetPoseToIdentity(ps);
        ps.last().set(sub.pose());

        TextureAtlasSprite sprite = sub.sprite();
        TextureAtlasSprite inferred = null;
        boolean skipInferredUvRemap = false;

        if (sprite != null) {
            var out = sprite.wrap(rec);

            Model<? super S> model = sub.model();
            model.setupAnim(sub.state());
            model.renderToBuffer(ps, out, sub.lightCoords(), sub.overlayCoords(), sub.tintedColor());

            rec.flush();
        } else {
            var tex = rt.tryExtractTextureId(rtObj);
            inferred = tex != null ? quadUtil.tryResolveEntitySprite(tex) : null;
            rec.setActiveSprite(inferred);

            Model<? super S> model = sub.model();
            model.setupAnim(sub.state());
            model.renderToBuffer(ps, rec, sub.lightCoords(), sub.overlayCoords(), sub.tintedColor());

            rec.flush();

            if (inferred != null && !skipInferredUvRemap) {
                rec.remapUvsToActiveSpriteIfPresent();
            }
        }
        TextureAtlasSprite finalSprite = sprite != null
                ? sprite
                : (inferred != null ? inferred : quadUtil.missingNoOrNull());
        emitter.emitRecordedAsQuads(rtObj, rec.vertices(), finalSprite, sub.tintedColor(), sink);
    }

    private void readModelPartSubmits(SubmitNodeCollection coll, GeometryBaker.Sink sink) {
        ModelPartFeatureRenderer.Storage storage = coll.getModelPartSubmits();
        ModelPartFeatureRendererStorageAccessor acc = (ModelPartFeatureRendererStorageAccessor) storage;
        for (var e : acc.getModelPartSubmits().entrySet()) {
            RenderType rtObj = e.getKey();
            for (SubmitNodeStorage.ModelPartSubmit sub : e.getValue()) {
                tessellateModelPartSubmit(rtObj, sub, sink);
            }
        }
    }

    private void tessellateModelPartSubmit(
            RenderType rtObj,
            SubmitNodeStorage.ModelPartSubmit sub,
            GeometryBaker.Sink sink
    ) {
        RecordingVertexConsumer rec = new RecordingVertexConsumer();

        PoseStack ps = new PoseStack();
        PackedQuadUtil.resetPoseToIdentity(ps);
        ps.last().set(sub.pose());

        TextureAtlasSprite sprite = sub.sprite();
        rec.setActiveSprite(sprite);

        ModelPart part = sub.modelPart();
        part.render(ps, rec, sub.lightCoords(), sub.overlayCoords(), sub.tintedColor());

        rec.flush();

        if (sprite != null) {
            rec.remapUvsToActiveSpriteIfPresent();
        }
        TextureAtlasSprite finalSprite = sprite != null ? sprite : quadUtil.missingNoOrNull();
        emitter.emitRecordedAsQuads(rtObj, rec.vertices(), finalSprite, sub.tintedColor(), sink);
    }

    private void readCustomGeometrySubmits(SubmitNodeCollection coll, GeometryBaker.Sink sink) {
        CustomFeatureRenderer.Storage storage = coll.getCustomGeometrySubmits();
        CustomFeatureRendererStorageAccessor acc = (CustomFeatureRendererStorageAccessor) storage;
        for (var e : acc.getCustomGeometrySubmits().entrySet()) {
            RenderType rtObj = e.getKey();
            for (SubmitNodeStorage.CustomGeometrySubmit sub : e.getValue()) {
                tessellateCustomSubmit(rtObj, sub, sink);
            }
        }
    }

    private void tessellateCustomSubmit(
            RenderType rtObj,
            SubmitNodeStorage.CustomGeometrySubmit sub,
            GeometryBaker.Sink sink
    ) {
        RecordingVertexConsumer rec = new RecordingVertexConsumer();

        PoseStack ps = new PoseStack();
        PackedQuadUtil.resetPoseToIdentity(ps);
        sub.customGeometryRenderer().render(sub.pose(), rec);
        rec.flush();
        emitter.emitRecordedAsTris(rtObj, rec.vertices(), sink);
    }
}
