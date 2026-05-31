package betterblockentities.client.render.immediate.blockentity.renderers;

/* local */
import betterblockentities.client.gui.config.ConfigCache;

/* minecraft */
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BellBlockEntity;

/* mojang */
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

public class BBEBellRenderer implements BlockEntityRenderer<BellBlockEntity> {
    public static final Material BELL_TEXTURE = new Material(TextureAtlas.LOCATION_BLOCKS, ResourceLocation.withDefaultNamespace("entity/bell/bell_body"));
    private final ModelPart bellBody;

    public BBEBellRenderer(BlockEntityRendererProvider.Context context) {
        this.bellBody = context.bakeLayer(ModelLayers.BELL).getChild("bell_body");
    }

    @Override public void render(final BellBlockEntity blockEntity, final float partialTick, final PoseStack poseStack, final MultiBufferSource vertexConsumers, final int light, final int overlay) {
        final float ticks = blockEntity.ticks + partialTick;
        float xRot = 0.0F;
        float zRot = 0.0F;

        final Direction shakeDirection = ConfigCache.bellAnims && blockEntity.shaking ? blockEntity.clickDirection : null;
        if (shakeDirection != null) {
            final float angle = Mth.sin(ticks / (float) Math.PI) / (4.0F + ticks / 3.0F);
            if (shakeDirection == Direction.NORTH) {
                xRot = -angle;
            } else if (shakeDirection == Direction.SOUTH) {
                xRot = angle;
            } else if (shakeDirection == Direction.EAST) {
                zRot = -angle;
            } else if (shakeDirection == Direction.WEST) {
                zRot = angle;
            }
        }

        this.bellBody.xRot = xRot;
        this.bellBody.zRot = zRot;

        final VertexConsumer consumer = BELL_TEXTURE.buffer(vertexConsumers, RenderType::entitySolid);
        this.bellBody.render(poseStack, consumer, light, overlay);
    }
}
