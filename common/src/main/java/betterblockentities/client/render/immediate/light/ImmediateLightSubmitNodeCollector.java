package betterblockentities.client.render.immediate.light;

/* mojang */
import com.mojang.blaze3d.vertex.PoseStack;

/* minecraft */
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.gizmos.DrawableGizmoPrimitives;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

/* java/misc */
import org.joml.Quaternionf;
import java.util.List;

/* annotations */
import org.jspecify.annotations.NonNull;

public final class ImmediateLightSubmitNodeCollector extends ImmediateLightOrderedSubmitNodeCollector implements SubmitNodeCollector {
    private final SubmitNodeCollector delegateRoot;

    public ImmediateLightSubmitNodeCollector(SubmitNodeCollector delegate, ImmediateBlockEntityLight.Parameters parameters) {
        super(delegate, parameters);
        this.delegateRoot = delegate;
    }

    @Override
    public @NonNull OrderedSubmitNodeCollector order(int order) {
        return new ImmediateLightOrderedSubmitNodeCollector(this.delegateRoot.order(order), this.parameters);
    }

    @Override
    public <S> void submitModel(
            @NonNull Model<? super S> model,
            @NonNull S state,
            @NonNull PoseStack poseStack,
            @NonNull RenderType renderType,
            int lightCoords,
            int overlayCoords,
            int tintedColor,
            TextureAtlasSprite sprite,
            int outlineColor,
            ModelFeatureRenderer.CrumblingOverlay crumblingOverlay
    ) {
        this.order(0).submitModel(
                model,
                state,
                poseStack,
                renderType,
                lightCoords,
                overlayCoords,
                tintedColor,
                sprite,
                outlineColor,
                crumblingOverlay
        );
    }
}

class ImmediateLightOrderedSubmitNodeCollector implements OrderedSubmitNodeCollector {
    protected final OrderedSubmitNodeCollector delegate;
    protected final ImmediateBlockEntityLight.Parameters parameters;

    ImmediateLightOrderedSubmitNodeCollector(OrderedSubmitNodeCollector delegate, ImmediateBlockEntityLight.Parameters parameters) {
        this.delegate = delegate;
        this.parameters = parameters;
    }

    @Override
    public void submitShadow(@NonNull PoseStack poseStack, float radius, @NonNull List<EntityRenderState.ShadowPiece> shadowPieces) {
        this.delegate.submitShadow(poseStack, radius, shadowPieces);
    }

    @Override
    public void submitNameTag(@NonNull PoseStack poseStack, Vec3 pos, int packedLight, @NonNull Component text, boolean seeThrough, int backgroundColor, @NonNull CameraRenderState camera) {
        this.delegate.submitNameTag(poseStack, pos, packedLight, text, seeThrough, backgroundColor, camera);
    }

    @Override
    public void submitText(@NonNull PoseStack poseStack, float x, float y, @NonNull FormattedCharSequence text, boolean dropShadow, Font.@NonNull DisplayMode displayMode, int color, int backgroundColor, int packedLight, int outlineColor) {
        this.delegate.submitText(poseStack, x, y, text, dropShadow, displayMode, color, backgroundColor, packedLight, outlineColor);
    }

    @Override
    public void submitFlame(@NonNull PoseStack poseStack, @NonNull EntityRenderState renderState, @NonNull Quaternionf quaternion) {
        this.delegate.submitFlame(poseStack, renderState, quaternion);
    }

    @Override
    public void submitLeash(@NonNull PoseStack poseStack, EntityRenderState.@NonNull LeashState leashState) {
        this.delegate.submitLeash(poseStack, leashState);
    }

    @Override
    public <S> void submitModel(
            @NonNull Model<? super S> model,
            @NonNull S state,
            @NonNull PoseStack poseStack,
            @NonNull RenderType renderType,
            int lightCoords,
            int overlayCoords,
            int tintedColor,
            TextureAtlasSprite sprite,
            int outlineColor,
            ModelFeatureRenderer.CrumblingOverlay crumblingOverlay
    ) {
        ImmediateBlockEntityLight.submitModel(
                this.delegate,
                this.parameters,
                model,
                state,
                poseStack,
                renderType,
                lightCoords,
                overlayCoords,
                tintedColor,
                sprite,
                outlineColor,
                crumblingOverlay
        );
    }

    @Override
    public void submitMovingBlock(@NonNull PoseStack poseStack, @NonNull MovingBlockRenderState renderState, int lightCoords) {
        this.delegate.submitMovingBlock(poseStack, renderState, lightCoords);
    }

    @Override
    public void submitBlockModel(@NonNull PoseStack poseStack, @NonNull RenderType renderType, @NonNull List<BlockStateModelPart> parts, int @NonNull [] tints, int lightCoords, int overlayCoords, int color) {
        this.delegate.submitBlockModel(poseStack, renderType, parts, tints, lightCoords, overlayCoords, color);
    }

    @Override
    public void submitBreakingBlockModel(@NonNull PoseStack poseStack, @NonNull List<BlockStateModelPart> parts, int progress) {
        this.delegate.submitBreakingBlockModel(poseStack, parts, progress);
    }

    @Override
    public void submitShapeOutline(@NonNull PoseStack poseStack, @NonNull VoxelShape shape, @NonNull RenderType renderType, int color, float lineWidth, boolean alwaysOnTop) {
        this.delegate.submitShapeOutline(poseStack, shape, renderType, color, lineWidth, alwaysOnTop);
    }

    @Override
    public void submitItem(
            @NonNull PoseStack poseStack,
            @NonNull ItemDisplayContext itemDisplayContext,
            int lightCoords,
            int overlayCoords,
            int color,
            int @NonNull [] tints,
            @NonNull List<BakedQuad> quads,
            ItemStackRenderState.@NonNull FoilType foilType
    ) {
        this.delegate.submitItem(poseStack, itemDisplayContext, lightCoords, overlayCoords, color, tints, quads, foilType);
    }

    @Override
    public void submitCustomGeometry(@NonNull PoseStack poseStack, @NonNull RenderType renderType, SubmitNodeCollector.@NonNull CustomGeometryRenderer renderer) {
        this.delegate.submitCustomGeometry(poseStack, renderType, renderer);
    }

    @Override
    public void submitQuadParticleGroup(@NonNull QuadParticleRenderState renderState) {
        this.delegate.submitQuadParticleGroup(renderState);
    }

    @Override
    public void submitGizmoPrimitives(DrawableGizmoPrimitives.@NonNull Group group, @NonNull CameraRenderState camera, boolean alwaysOnTop) {
        this.delegate.submitGizmoPrimitives(group, camera, alwaysOnTop);
    }
}
