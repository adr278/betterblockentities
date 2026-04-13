package betterblockentities.client.chunk.pipeline.capture;

/* local */
import betterblockentities.client.chunk.pipeline.shelf.GeometryBaker;
import betterblockentities.client.chunk.pipeline.shelf.PackedQuadUtil;
import betterblockentities.client.chunk.pipeline.shelf.RenderTypeClassifier;
import betterblockentities.client.model.geometry.ModelUtility;

/* mojang */
import com.mojang.blaze3d.vertex.PoseStack;

/* minecraft */
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;

/* annotations */
import org.jspecify.annotations.NonNull;

/* joml */
import org.joml.Matrix4f;
import org.joml.Quaternionf;

/* java */
import java.util.ArrayList;
import java.util.List;

public final class SubmitNodeGeometryCaptureCollector extends SubmitNodeStorage {
    private final RenderTypeClassifier rt;
    private final PackedQuadUtil quadUtil;

    private final PoseStack copiedPose = new PoseStack();
    private final ArrayList<BakedQuad> scratchQuads = new ArrayList<>(32);

    private GeometryBaker.Sink sink;
    private boolean supported;
    private boolean rejectGlintGeometry;

    public SubmitNodeGeometryCaptureCollector(
            RenderTypeClassifier rt,
            PackedQuadUtil quadUtil
    ) {
        this.rt = rt;
        this.quadUtil = quadUtil;
    }

    public void reset(GeometryBaker.Sink sink) {
        reset(sink, false);
    }

    public void reset(GeometryBaker.Sink sink, boolean rejectGlintGeometry) {
        this.sink = sink;
        this.supported = true;
        this.rejectGlintGeometry = rejectGlintGeometry;
        this.scratchQuads.clear();
        PackedQuadUtil.resetPoseToIdentity(this.copiedPose);
    }

    public boolean supported() {
        return this.supported;
    }

    private void unsupported() {
        this.supported = false;
    }

    private void copyPoseFrom(PoseStack source) {
        PackedQuadUtil.resetPoseToIdentity(this.copiedPose);
        this.copiedPose.last().set(source.last());
    }

    private TextureAtlasSprite resolveSprite(RenderType renderType, TextureAtlasSprite sprite) {
        if (sprite != null) return sprite;

        Identifier tex = this.rt.tryExtractTextureId(renderType);
        TextureAtlasSprite resolved = tex != null ? this.quadUtil.tryResolveEntitySprite(tex) : null;
        return resolved != null ? resolved : this.quadUtil.missingNoOrNull();
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
        if (this.sink == null || !this.supported) return;

        TextureAtlasSprite resolvedSprite = resolveSprite(renderType, sprite);

        copyPoseFrom(poseStack);
        this.scratchQuads.clear();
        ModelUtility.toBakedQuads(part, this.scratchQuads, resolvedSprite, this.copiedPose);

        int[] tintLayers = tintLayersFromColor(tintColor);
        Object layerHint = new RenderTypeClassifier.SpriteAwareRenderType(
                resolvedSprite,
                String.valueOf(renderType)
        );

        for (BakedQuad quad : this.scratchQuads) {
            this.sink.accept(
                    PackedQuadUtil.transformQuadToPacked(quad),
                    layerHint,
                    tintLayers
            );
        }
    }

    private void emitBlockModelAsQuads(
            PoseStack poseStack,
            RenderType renderType,
            List<BlockStateModelPart> parts,
            int[] tintLayers
    ) {
        if (this.sink == null || !this.supported) return;

        Matrix4f pose = poseStack.last().pose();

        for (BlockStateModelPart part : parts) {
            emitBlockModelFace(renderType, tintLayers, pose, part, null);

            for (Direction direction : Direction.values()) {
                emitBlockModelFace(renderType, tintLayers, pose, part, direction);
            }
        }
    }

    private void emitBlockModelFace(
            RenderType renderType,
            int[] tintLayers,
            Matrix4f pose,
            BlockStateModelPart part,
            Direction cullFace
    ) {
        for (BakedQuad quad : part.getQuads(cullFace)) {
            Object layerHint = new RenderTypeClassifier.SpriteAwareRenderType(
                    quad.materialInfo().sprite(),
                    String.valueOf(renderType)
            );

            this.sink.accept(
                    PackedQuadUtil.transformQuadToPacked(quad, pose),
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
    ) { emitModelPartAsQuads(model.root(), poseStack, renderType, sprite, tintColor); }

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
        if (this.rejectGlintGeometry && renderGlint) {
            unsupported();
            return;
        }

        emitModelPartAsQuads(modelPart, poseStack, renderType, sprite, tintColor);
    }

    @Override public void submitBlockModel(
            @NonNull PoseStack poseStack,
            @NonNull RenderType renderType,
            @NonNull List<BlockStateModelPart> parts,
            int @NonNull [] tintLayers,
            int packedLight,
            int packedOverlay,
            int outlineColor
    ) { emitBlockModelAsQuads(poseStack, renderType, parts, tintLayers); }

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
        if (this.sink == null || !this.supported) return;
        if (this.rejectGlintGeometry && foilType != ItemStackRenderState.FoilType.NONE) {
            unsupported();
            return;
        }

        Matrix4f pose = poseStack.last().pose();

        for (BakedQuad quad : quads) {
            Object layerHint = new RenderTypeClassifier.SpriteAwareRenderType(
                    quad.materialInfo().sprite(),
                    String.valueOf(quad.materialInfo().layer())
            );

            this.sink.accept(
                    PackedQuadUtil.transformQuadToPacked(quad, pose),
                    layerHint,
                    tintLayers
            );
        }
    }

    @Override public void submitMovingBlock(@NonNull PoseStack poseStack, @NonNull MovingBlockRenderState movingBlockRenderState)
    { unsupported(); }

    @Override public void submitBreakingBlockModel(
            @NonNull PoseStack poseStack,
            @NonNull BlockStateModel blockStateModel,
            long seed,
            int color
    ) { unsupported(); }

    @Override public void submitCustomGeometry(
            @NonNull PoseStack poseStack,
            @NonNull RenderType renderType,
            SubmitNodeCollector.@NonNull CustomGeometryRenderer customGeometryRenderer
    ) { unsupported(); }

    @Override public void submitParticleGroup(SubmitNodeCollector.@NonNull ParticleGroupRenderer particleGroupRenderer)
    { unsupported(); }

    @Override public void submitShadow(
            @NonNull PoseStack poseStack,
            float strength,
            @NonNull List<EntityRenderState.ShadowPiece> pieces
    ) { unsupported(); }

    @Override public void submitNameTag(
            @NonNull PoseStack poseStack,
            Vec3 attachment,
            int backgroundColor,
            @NonNull Component text,
            boolean drawBackground,
            int light,
            double distanceToCameraSq,
            @NonNull CameraRenderState camera
    ) { unsupported(); }

    @Override public void submitText(
            @NonNull PoseStack poseStack,
            float x,
            float y,
            @NonNull FormattedCharSequence formattedCharSequence,
            boolean shadow,
            Font.@NonNull DisplayMode displayMode,
            int color,
            int backgroundColor,
            int light,
            int outlineColor
    ) { unsupported(); }

    @Override public void submitFlame(
            @NonNull PoseStack poseStack,
            @NonNull EntityRenderState renderState,
            @NonNull Quaternionf quaternion
    ) { unsupported(); }

    @Override public void submitLeash(
            @NonNull PoseStack poseStack,
            EntityRenderState.@NonNull LeashState leashState
    ) { unsupported(); }
}
