package betterblockentities.client.render.immediate.blockentity.renderers;

/* minecraft */
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

/* mojang */
import com.mojang.blaze3d.vertex.PoseStack;

public class BBEDummyRenderer implements BlockEntityRenderer<BlockEntity> {
    @Override public void render(
            final BlockEntity blockEntity,
            final float partialTick,
            final PoseStack poseStack,
            final MultiBufferSource vertexConsumers,
            final int light,
            final int overlay
    ) {}

    @Override public boolean shouldRenderOffScreen(final BlockEntity blockEntity) {
        return false;
    }

    @Override public int getViewDistance() {
        return 0;
    }

    @Override public boolean shouldRender(final BlockEntity blockEntity, final Vec3 cameraPosition) {
        return false;
    }
}
