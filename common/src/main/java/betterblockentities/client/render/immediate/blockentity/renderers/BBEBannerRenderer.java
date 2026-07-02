package betterblockentities.client.render.immediate.blockentity.renderers;

/* local */
import betterblockentities.client.gui.config.ConfigCache;
import betterblockentities.client.gui.option.EnumTypes;

/* minecraft */
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BannerBlock;
import net.minecraft.world.level.block.WallBannerBlock;
import net.minecraft.world.level.block.entity.BannerBlockEntity;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import net.minecraft.world.level.block.entity.BannerPatternLayers.Layer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RotationSegment;

/* mojang */
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

public class BBEBannerRenderer implements BlockEntityRenderer<BannerBlockEntity> {
    private static final int MAX_PATTERNS = 16;
    private static final float MODEL_SCALE = 0.6666667F;
    private static final String FLAG = "flag";
    private static final String POLE = "pole";
    private static final String BAR = "bar";

    private final ModelPart flag;
    private final ModelPart pole;
    private final ModelPart bar;

    public BBEBannerRenderer(final BlockEntityRendererProvider.Context context) {
        final ModelPart root = context.bakeLayer(ModelLayers.BANNER);
        this.flag = root.getChild(FLAG);
        this.pole = root.getChild(POLE);
        this.bar = root.getChild(BAR);
    }

    @Override public void render(final BannerBlockEntity blockEntity, final float partialTick, final PoseStack poseStack, final MultiBufferSource vertexConsumers, final int light, final int overlay) {
        final Level level = blockEntity.getLevel();
        final boolean noLevel = level == null;
        long gameTime;

        poseStack.pushPose();
        if (noLevel) {
            gameTime = 0L;
            poseStack.translate(0.5F, 0.5F, 0.5F);
            this.pole.visible = true;
        } else {
            gameTime = level.getGameTime();
            final BlockState blockState = blockEntity.getBlockState();
            if (blockState.getBlock() instanceof BannerBlock) {
                poseStack.translate(0.5F, 0.5F, 0.5F);
                final float rotation = -RotationSegment.convertToDegrees(blockState.getValue(BannerBlock.ROTATION));
                poseStack.mulPose(Axis.YP.rotationDegrees(rotation));
                this.pole.visible = true;
            } else {
                poseStack.translate(0.5F, -0.16666667F, 0.5F);
                final float rotation = -blockState.getValue(WallBannerBlock.FACING).toYRot();
                poseStack.mulPose(Axis.YP.rotationDegrees(rotation));
                poseStack.translate(0.0F, -0.3125F, -0.4375F);
                this.pole.visible = false;
            }
        }

        poseStack.pushPose();
        poseStack.scale(MODEL_SCALE, -MODEL_SCALE, -MODEL_SCALE);

        final VertexConsumer baseConsumer = ModelBakery.BANNER_BASE.buffer(vertexConsumers, RenderType::entitySolid);
        this.pole.render(poseStack, baseConsumer, light, overlay);
        this.bar.render(poseStack, baseConsumer, light, overlay);

        final BlockPos blockPos = blockEntity.getBlockPos();
        final float phase = ((float) Math.floorMod(blockPos.getX() * 7L + blockPos.getY() * 9L + blockPos.getZ() * 13L + gameTime, 100L) + partialTick) / 100.0F;
        final float step = -0.45F;
        final float rotation = step * ConfigCache.bannerPose;
        final float clamped = Mth.clamp(rotation, -4.05F, -0.45F);
        this.flag.xRot = (float) Math.toRadians(clamped);
        this.flag.y = -32.0F;

        renderPatterns(
                poseStack,
                vertexConsumers,
                light,
                overlay,
                this.flag,
                ModelBakery.BANNER_BASE,
                true,
                blockEntity.getBaseColor(),
                blockEntity.getPatterns()
        );

        poseStack.popPose();
        poseStack.popPose();
    }

    public static void renderPatterns(
            final PoseStack poseStack,
            final MultiBufferSource vertexConsumers,
            final int light,
            final int overlay,
            final ModelPart flag,
            final Material baseMaterial,
            final boolean banner,
            final DyeColor baseColor,
            final BannerPatternLayers patternLayers
    ) {
        renderPatterns(poseStack, vertexConsumers, light, overlay, flag, baseMaterial, banner, baseColor, patternLayers, false);
    }

    public static void renderPatterns(
            final PoseStack poseStack,
            final MultiBufferSource vertexConsumers,
            final int light,
            final int overlay,
            final ModelPart flag,
            final Material baseMaterial,
            final boolean banner,
            final DyeColor baseColor,
            final BannerPatternLayers patternLayers,
            final boolean glint
    ) {
        flag.render(poseStack, baseMaterial.buffer(vertexConsumers, RenderType::entitySolid, glint), light, overlay);
        renderPatternLayer(
                poseStack,
                vertexConsumers,
                light,
                overlay,
                flag,
                banner ? Sheets.BANNER_BASE : Sheets.SHIELD_BASE,
                baseColor
        );

        for (int i = 0; i < MAX_PATTERNS && i < patternLayers.layers().size(); i++) {
            final Layer layer = patternLayers.layers().get(i);
            final Material material = banner ? Sheets.getBannerMaterial(layer.pattern()) : Sheets.getShieldMaterial(layer.pattern());
            renderPatternLayer(poseStack, vertexConsumers, light, overlay, flag, material, layer.color());
        }
    }

    private static void renderPatternLayer(
            final PoseStack poseStack,
            final MultiBufferSource vertexConsumers,
            final int light,
            final int overlay,
            final ModelPart flag,
            final Material material,
            final DyeColor color
    ) {
        if (ConfigCache.bannerGraphics == EnumTypes.BannerGraphicsType.FAST.ordinal()) {
            flag.render(poseStack, material.buffer(vertexConsumers, RenderType::entityCutoutNoCull), light, overlay, color.getTextureDiffuseColor());
            return;
        }

        flag.render(poseStack, material.buffer(vertexConsumers, RenderType::entityNoOutline), light, overlay, color.getTextureDiffuseColor());
    }
}
