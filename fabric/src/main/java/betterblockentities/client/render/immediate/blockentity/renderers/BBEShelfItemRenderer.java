package betterblockentities.client.render.immediate.blockentity.renderers;

/* local */
import betterblockentities.client.chunk.pipeline.shelf.GeometryBaker;
import betterblockentities.client.chunk.pipeline.shelf.PackedQuadUtil;
import betterblockentities.client.chunk.pipeline.shelf.RenderTypeClassifier;
import betterblockentities.client.model.geometry.ModelUtility;

/* mojang */
import com.mojang.blaze3d.vertex.PoseStack;

/* minecraft */
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;

/* annotations */
import org.jspecify.annotations.NonNull;

/* java */
import java.util.ArrayList;
import java.util.List;

public final class BBEShelfItemRenderer extends SubmitNodeStorage {
    private final RenderTypeClassifier rt;
    private final PackedQuadUtil quadUtil;

    private final PoseStack copiedPose = new PoseStack();
    private final ArrayList<BakedQuad> scratchQuads = new ArrayList<>(32);

    private GeometryBaker.Sink sink;

    public BBEShelfItemRenderer(
            RenderTypeClassifier rt,
            PackedQuadUtil quadUtil
    ) {
        this.rt = rt;
        this.quadUtil = quadUtil;
    }

    public void reset(GeometryBaker.Sink sink) {
        this.sink = sink;
        scratchQuads.clear();
        PackedQuadUtil.resetPoseToIdentity(copiedPose);
    }

    private void copyPoseFrom(PoseStack source) {
        PackedQuadUtil.resetPoseToIdentity(copiedPose);
        copiedPose.last().set(source.last());
    }

    private TextureAtlasSprite resolveSprite(RenderType renderType, TextureAtlasSprite sprite) {
        if (sprite != null) return sprite;

        Identifier tex = rt.tryExtractTextureId(renderType);
        TextureAtlasSprite resolved = tex != null ? quadUtil.tryResolveEntitySprite(tex) : null;
        return resolved != null ? resolved : quadUtil.missingNoOrNull();
    }

    private static int[] tintLayersFromColor(int tintColor) {
        return tintColor == -1 ? null : new int[]{tintColor};
    }

    private void emitModelPartAsQuads(
            ModelPart part,
            PoseStack poseStack,
            RenderType renderType,
            TextureAtlasSprite sprite,
            int tintColor
    ) {
        if (sink == null) return;

        TextureAtlasSprite resolvedSprite = resolveSprite(renderType, sprite);

        copyPoseFrom(poseStack);
        scratchQuads.clear();

        ModelUtility.toBakedQuads(part, scratchQuads, resolvedSprite, copiedPose);

        int[] tintLayers = tintLayersFromColor(tintColor);

        Object layerHint = new RenderTypeClassifier.SpriteAwareRenderType(
                resolvedSprite,
                String.valueOf(renderType)
        );

        for (BakedQuad quad : scratchQuads) {
            sink.accept(
                    PackedQuadUtil.transformQuadToPacked(quad),
                    layerHint,
                    tintLayers
            );
        }
    }

    @Override public <S> void submitModel(
            @NonNull Model<? super S> model,
            @NonNull S renderState,
            @NonNull PoseStack poseStack,
            @NonNull RenderType renderType,
            int packedLight,
            int packedOverlay,
            int tintColor,
            TextureAtlasSprite sprite,
            int outlineColor,
            ModelFeatureRenderer.CrumblingOverlay crumblingOverlay
    ) {
        emitModelPartAsQuads(model.root(), poseStack, renderType, sprite, tintColor);
    }

    @Override public void submitModelPart(
            @NonNull ModelPart modelPart,
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
        emitModelPartAsQuads(modelPart, poseStack, renderType, sprite, tintColor);
    }

    @Override public void submitItem(
            @NonNull PoseStack poseStack,
            @NonNull ItemDisplayContext itemDisplayContext,
            int packedLight,
            int packedOverlay,
            int outlineColor,
            int @NonNull [] tintLayers,
            @NonNull List<BakedQuad> quads,
            ItemStackRenderState.@NonNull FoilType foilType
    ) {
        if (sink == null) return;

        var pose = poseStack.last().pose();

        for (BakedQuad quad : quads) {
            Object layerHint = new RenderTypeClassifier.SpriteAwareRenderType(
                    quad.materialInfo().sprite(),
                    String.valueOf(quad.materialInfo().layer())
            );

            sink.accept(
                    PackedQuadUtil.transformQuadToPacked(quad, pose),
                    layerHint,
                    tintLayers
            );
        }
    }
}
