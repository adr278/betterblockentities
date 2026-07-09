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
import net.minecraft.client.renderer.blockentity.BrightnessCombiner;
import net.minecraft.client.resources.model.Material;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.DoubleBlockCombiner;
import net.minecraft.world.level.block.entity.BedBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;

/* mojang */
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

public class BBEBedRenderer implements BlockEntityRenderer<BedBlockEntity> {
    private final ModelPart headRoot;
    private final ModelPart footRoot;

    public BBEBedRenderer(final BlockEntityRendererProvider.Context context) {
        this.headRoot = context.bakeLayer(ModelLayers.BED_HEAD);
        this.footRoot = context.bakeLayer(ModelLayers.BED_FOOT);
    }

    @Override public void render(final BedBlockEntity blockEntity, final float partialTick, final PoseStack poseStack, final MultiBufferSource vertexConsumers, final int light, final int overlay) {
        final Material material = ModelResourceUtil.getBedMaterial(blockEntity.getBlockState(), blockEntity.getColor());
        final Level level = blockEntity.getLevel();

        if (level != null) {
            final BlockState blockState = blockEntity.getBlockState();
            final DoubleBlockCombiner.NeighborCombineResult<? extends BedBlockEntity> combineResult = DoubleBlockCombiner.combineWithNeigbour(
                    BlockEntityType.BED,
                    BedBlock::getBlockType,
                    BedBlock::getConnectedDirection,
                    ChestBlock.FACING,
                    blockState,
                    level,
                    blockEntity.getBlockPos(),
                    (levelAccessor, blockPos) -> false
            );
            final int packedLight = combineResult.apply(new BrightnessCombiner<>()).get(light);
            final ModelPart partRoot = blockState.getValue(BedBlock.PART) == BedPart.HEAD ? this.headRoot : this.footRoot;
            final Direction facing = blockState.getValue(BedBlock.FACING);
            renderPiece(poseStack, vertexConsumers, partRoot, facing, material, packedLight, overlay, false);
        } else {
            renderPiece(poseStack, vertexConsumers, this.headRoot, Direction.SOUTH, material, light, overlay, false);
            renderPiece(poseStack, vertexConsumers, this.footRoot, Direction.SOUTH, material, light, overlay, true);
        }
    }

    private static void renderPiece(
            final PoseStack poseStack,
            final MultiBufferSource vertexConsumers,
            final ModelPart modelRoot,
            final Direction facing,
            final Material material,
            final int light,
            final int overlay,
            final boolean foot
    ) {
        poseStack.pushPose();
        poseStack.translate(0.0F, 0.5625F, foot ? -1.0F : 0.0F);
        poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
        poseStack.translate(0.5F, 0.5F, 0.5F);
        poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F + facing.toYRot()));
        poseStack.translate(-0.5F, -0.5F, -0.5F);

        final VertexConsumer consumer = material.buffer(vertexConsumers, RenderType::entitySolid);
        modelRoot.render(poseStack, consumer, light, overlay);
        poseStack.popPose();
    }
}
