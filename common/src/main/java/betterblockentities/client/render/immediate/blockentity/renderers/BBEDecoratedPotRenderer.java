package betterblockentities.client.render.immediate.blockentity.renderers;

/* local */
import betterblockentities.client.chunk.util.ModelResourceUtil;

/* minecraft */
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.Material;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.DecoratedPotBlockEntity;
import net.minecraft.world.level.block.entity.DecoratedPotBlockEntity.WobbleStyle;
import net.minecraft.world.level.block.entity.PotDecorations;

/* mojang */
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

public class BBEDecoratedPotRenderer implements BlockEntityRenderer<DecoratedPotBlockEntity> {
    private final ModelPart neck;
    private final ModelPart frontSide;
    private final ModelPart backSide;
    private final ModelPart leftSide;
    private final ModelPart rightSide;
    private final ModelPart top;
    private final ModelPart bottom;

    public BBEDecoratedPotRenderer(final BlockEntityRendererProvider.Context context) {
        final ModelPart base = context.bakeLayer(ModelLayers.DECORATED_POT_BASE);
        this.neck = base.getChild("neck");
        this.top = base.getChild("top");
        this.bottom = base.getChild("bottom");

        final ModelPart sides = context.bakeLayer(ModelLayers.DECORATED_POT_SIDES);
        this.frontSide = sides.getChild("front");
        this.backSide = sides.getChild("back");
        this.leftSide = sides.getChild("left");
        this.rightSide = sides.getChild("right");
    }

    @Override public void render(
            final DecoratedPotBlockEntity blockEntity,
            final float partialTick,
            final PoseStack poseStack,
            final MultiBufferSource vertexConsumers,
            final int light,
            final int overlay
    ) {
        poseStack.pushPose();

        final Direction direction = blockEntity.getDirection();
        poseStack.translate(0.5D, 0.0D, 0.5D);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - direction.toYRot()));
        poseStack.translate(-0.5D, 0.0D, -0.5D);

        final WobbleStyle wobbleStyle = blockEntity.lastWobbleStyle;
        if (wobbleStyle != null && blockEntity.getLevel() != null) {
            final float wobbleProgress = ((float) (blockEntity.getLevel().getGameTime() - blockEntity.wobbleStartedAtTick) + partialTick) / wobbleStyle.duration;
            if (wobbleProgress >= 0.0F && wobbleProgress <= 1.0F) {
                if (wobbleStyle == WobbleStyle.POSITIVE) {
                    final float phase = wobbleProgress * ((float) Math.PI * 2.0F);
                    final float xRot = -1.5F * (Mth.cos(phase) + 0.5F) * Mth.sin(phase / 2.0F);
                    poseStack.rotateAround(Axis.XP.rotation(xRot * 0.015625F), 0.5F, 0.0F, 0.5F);
                    final float zRot = Mth.sin(phase);
                    poseStack.rotateAround(Axis.ZP.rotation(zRot * 0.015625F), 0.5F, 0.0F, 0.5F);
                } else {
                    final float yRot = Mth.sin(-wobbleProgress * 3.0F * (float) Math.PI) * 0.125F;
                    final float decay = 1.0F - wobbleProgress;
                    poseStack.rotateAround(Axis.YP.rotation(yRot * decay), 0.5F, 0.0F, 0.5F);
                }
            }
        }

        final var baseConsumer = ModelResourceUtil.getDecoratedPotBaseMaterial().buffer(vertexConsumers, RenderType::entitySolid);
        this.neck.render(poseStack, baseConsumer, light, overlay);
        this.top.render(poseStack, baseConsumer, light, overlay);
        this.bottom.render(poseStack, baseConsumer, light, overlay);

        final PotDecorations decorations = blockEntity.getDecorations();
        renderSide(this.frontSide, poseStack, vertexConsumers, light, overlay, ModelResourceUtil.getPotSideMaterial(decorations.front()));
        renderSide(this.backSide, poseStack, vertexConsumers, light, overlay, ModelResourceUtil.getPotSideMaterial(decorations.back()));
        renderSide(this.leftSide, poseStack, vertexConsumers, light, overlay, ModelResourceUtil.getPotSideMaterial(decorations.left()));
        renderSide(this.rightSide, poseStack, vertexConsumers, light, overlay, ModelResourceUtil.getPotSideMaterial(decorations.right()));

        poseStack.popPose();
    }

    private void renderSide(
            final ModelPart side,
            final PoseStack poseStack,
            final MultiBufferSource vertexConsumers,
            final int light,
            final int overlay,
            final Material material
    ) {
        side.render(poseStack, material.buffer(vertexConsumers, RenderType::entitySolid), light, overlay);
    }
}
