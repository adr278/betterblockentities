package betterblockentities.client.render.immediate.blockentity.renderers;

/* local */
import betterblockentities.client.gui.config.ConfigCache;
import betterblockentities.client.model.overrides.ChestModelOverride;
import betterblockentities.client.render.immediate.blockentity.misc.CrumblingOverlayConsumer;

/* minecraft */
import betterblockentities.platform.GlobalScope;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.BrightnessCombiner;
import net.minecraft.client.resources.model.Material;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractChestBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.DoubleBlockCombiner;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.LidBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;

/* mojang */
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

/* java/misc */
import java.util.Calendar;

public class BBEChestRenderer<T extends BlockEntity & LidBlockEntity> implements BlockEntityRenderer<T> {
    private static final String BOTTOM = "bottom";
    private static final String LID = "lid";
    private static final String LOCK = "lock";

    private final ModelPart lid;
    private final ModelPart bottom;
    private final ModelPart lock;
    private final ModelPart doubleLeftLid;
    private final ModelPart doubleLeftBottom;
    private final ModelPart doubleLeftLock;
    private final ModelPart doubleRightLid;
    private final ModelPart doubleRightBottom;
    private final ModelPart doubleRightLock;

    //bbe overrides
    private ModelPart bbeLid;
    private ModelPart bbeBottom;
    private ModelPart bbeLock;
    private ModelPart bbeDoubleLeftLid;
    private ModelPart bbeDoubleLeftBottom;
    private ModelPart bbeDoubleLeftLock;
    private ModelPart bbeDoubleRightLid;
    private ModelPart bbeDoubleRightBottom;
    private ModelPart bbeDoubleRightLock;

    private final boolean xmasTextures;

    public BBEChestRenderer(final BlockEntityRendererProvider.Context context) {
        this.xmasTextures = ConfigCache.christmasChests || isExtendedChristmas();

        final ModelPart chest = context.bakeLayer(ModelLayers.CHEST);
        this.bottom = chest.getChild(BOTTOM);
        this.lid = chest.getChild(LID);
        this.lock = chest.getChild(LOCK);

        final ModelPart doubleLeft = context.bakeLayer(ModelLayers.DOUBLE_CHEST_LEFT);
        this.doubleLeftBottom = doubleLeft.getChild(BOTTOM);
        this.doubleLeftLid = doubleLeft.getChild(LID);
        this.doubleLeftLock = doubleLeft.getChild(LOCK);

        final ModelPart doubleRight = context.bakeLayer(ModelLayers.DOUBLE_CHEST_RIGHT);
        this.doubleRightBottom = doubleRight.getChild(BOTTOM);
        this.doubleRightLid = doubleRight.getChild(LID);
        this.doubleRightLock = doubleRight.getChild(LOCK);

        initBBBModelOverrides(context);
    }


    public void initBBBModelOverrides(BlockEntityRendererProvider.Context context) {
        final ModelPart chest = context.bakeLayer(ModelLayers.CHEST);
        this.bbeBottom = chest.getChild(BOTTOM);
        this.bbeLid = chest.getChild(LID);
        this.bbeLock = chest.getChild(LOCK);

        final ModelPart doubleLeft = context.bakeLayer(ModelLayers.DOUBLE_CHEST_LEFT);
        this.bbeDoubleLeftBottom = doubleLeft.getChild(BOTTOM);
        this.bbeDoubleLeftLid = doubleLeft.getChild(LID);
        this.bbeDoubleLeftLock = doubleLeft.getChild(LOCK);

        final ModelPart doubleRight = context.bakeLayer(ModelLayers.DOUBLE_CHEST_RIGHT);
        this.bbeDoubleRightBottom = doubleRight.getChild(BOTTOM);
        this.bbeDoubleRightLid = doubleRight.getChild(LID);
        this.bbeDoubleRightLock = doubleRight.getChild(LOCK);

        ChestModelOverride.splitModel(this.bbeBottom);
        ChestModelOverride.splitModel(this.bbeDoubleLeftBottom);
        ChestModelOverride.splitModel(this.bbeDoubleRightBottom);
    }

    @Override public void render(final T blockEntity, final float partialTick, final PoseStack poseStack, final MultiBufferSource vertexConsumers, final int light, final int overlay) {
        final Level level = blockEntity.getLevel();
        final boolean hasLevel = level != null;

        final BlockState blockState = hasLevel
                ? blockEntity.getBlockState()
                : Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.SOUTH);

        final ChestType chestType = blockState.hasProperty(ChestBlock.TYPE)
                ? blockState.getValue(ChestBlock.TYPE)
                : ChestType.SINGLE;

        final Block block = blockState.getBlock();
        if (!(block instanceof AbstractChestBlock<?> abstractChestBlock)) {
            return;
        }

        final boolean isDouble = chestType != ChestType.SINGLE;

        poseStack.pushPose();
        final float angle = blockState.getValue(ChestBlock.FACING).toYRot();
        poseStack.translate(0.5F, 0.5F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(-angle));
        poseStack.translate(-0.5F, -0.5F, -0.5F);

        final DoubleBlockCombiner.NeighborCombineResult<? extends ChestBlockEntity> combineResult = hasLevel
                ? abstractChestBlock.combine(blockState, level, blockEntity.getBlockPos(), true)
                : DoubleBlockCombiner.Combiner::acceptNone;

        float openness = ConfigCache.chestAnims
                ? combineResult.apply(ChestBlock.opennessCombiner(blockEntity)).get(partialTick)
                : 0.0F;
        openness = 1.0F - openness;
        openness = 1.0F - openness * openness * openness;

        final int packedLight = combineResult.apply(new BrightnessCombiner<>()).applyAsInt(light);
        final Material material = Sheets.chooseMaterial(blockEntity, chestType, this.xmasTextures);
        final VertexConsumer consumer = material.buffer(vertexConsumers, RenderType::entityCutout);

        if (isDouble) {
            if (chestType == ChestType.LEFT) {
                if (vertexConsumers instanceof CrumblingOverlayConsumer.CrumblingOnlyBufferSource) {
                    renderModel(poseStack, consumer, this.doubleLeftLid, this.doubleLeftLock, this.doubleLeftBottom, openness, packedLight, overlay);
                }
                else {
                    renderModel(poseStack, consumer, this.bbeDoubleLeftLid, this.bbeDoubleLeftLock, this.bbeDoubleLeftBottom, openness, packedLight, overlay);
                }
            } else {
                if (vertexConsumers instanceof CrumblingOverlayConsumer.CrumblingOnlyBufferSource) {
                    renderModel(poseStack, consumer, this.doubleRightLid, this.doubleRightLock, this.doubleRightBottom, openness, packedLight, overlay);
                }
                else {
                    renderModel(poseStack, consumer, this.bbeDoubleRightLid, this.bbeDoubleRightLock, this.bbeDoubleRightBottom, openness, packedLight, overlay);
                }
            }
        } else {
            if (vertexConsumers instanceof CrumblingOverlayConsumer.CrumblingOnlyBufferSource || GlobalScope.isItemInvoked) {
                renderModel(poseStack, consumer, this.lid, this.lock, this.bottom, openness, packedLight, overlay);
            }
            else {
                renderModel(poseStack, consumer, this.bbeLid, this.bbeLock, this.bbeBottom, openness, packedLight, overlay);
            }
        }
        poseStack.popPose();
    }

    private void renderModel(
            final PoseStack poseStack,
            final VertexConsumer consumer,
            final ModelPart lid,
            final ModelPart lock,
            final ModelPart bottom,
            final float openness,
            final int light,
            final int overlay
    ) {
        lid.xRot = -(openness * ((float) Math.PI / 2F));
        lock.xRot = lid.xRot;
        lid.render(poseStack, consumer, light, overlay);
        lock.render(poseStack, consumer, light, overlay);
        bottom.render(poseStack, consumer, light, overlay);
    }

    private static boolean isExtendedChristmas() {
        final Calendar calendar = Calendar.getInstance();
        final int month = calendar.get(Calendar.MONTH) + 1;
        final int day = calendar.get(Calendar.DAY_OF_MONTH);
        return month == 12 && day >= 24 && day <= 26;
    }
}
