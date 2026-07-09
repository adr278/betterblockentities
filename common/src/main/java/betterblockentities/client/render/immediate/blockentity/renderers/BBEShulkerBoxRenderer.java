package betterblockentities.client.render.immediate.blockentity.renderers;

/* local */
import betterblockentities.client.gui.config.ConfigCache;
import betterblockentities.client.chunk.util.ModelResourceUtil;

/* minecraft */
import net.minecraft.client.model.ShulkerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/* mojang */
import com.mojang.blaze3d.vertex.PoseStack;

public class BBEShulkerBoxRenderer implements BlockEntityRenderer<ShulkerBoxBlockEntity> {
    private final ShulkerModel<?> model;

    public BBEShulkerBoxRenderer(final BlockEntityRendererProvider.Context context) {
        this.model = new ShulkerModel<>(context.bakeLayer(ModelLayers.SHULKER));
    }

    @Override public void render(
            final ShulkerBoxBlockEntity blockEntity,
            final float partialTick,
            final PoseStack poseStack,
            final MultiBufferSource vertexConsumers,
            final int light,
            final int overlay
    ) {
        BlockState blockState;
        if (blockEntity.hasLevel()) {
            assert blockEntity.getLevel() != null;
            blockState = blockEntity.getLevel().getBlockState(blockEntity.getBlockPos());
        } else {
            blockState = blockEntity.getBlockState();
        }

        Direction direction = Direction.UP;
        if (blockState.getBlock() instanceof ShulkerBoxBlock) {
            direction = blockState.getValue(ShulkerBoxBlock.FACING);
        }

        final DyeColor color = blockEntity.getColor();
        final var material = ModelResourceUtil.getShulkerMaterial(blockState, color);

        poseStack.pushPose();
        poseStack.translate(0.5F, 0.5F, 0.5F);
        poseStack.scale(0.9995F, 0.9995F, 0.9995F);
        poseStack.mulPose(direction.getRotation());
        poseStack.scale(1.0F, -1.0F, -1.0F);
        poseStack.translate(0.0F, -1.0F, 0.0F);

        final float progress = ConfigCache.shulkerAnims ? blockEntity.getProgress(partialTick) : 0.0F;
        final ModelPart lid = this.model.getLid();
        lid.setPos(0.0F, 24.0F - progress * 0.5F * 16.0F, 0.0F);
        lid.yRot = 270.0F * progress * ((float) Math.PI / 180F);

        this.model.renderToBuffer(poseStack, material.buffer(vertexConsumers, RenderType::entityCutoutNoCull), light, overlay);
        poseStack.popPose();
    }
}
