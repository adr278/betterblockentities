package betterblockentities.client.chunk.pipeline.itemframe;

/* mojang */
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

/* minecraft */
import net.minecraft.client.renderer.entity.ItemFrameRenderer;
import net.minecraft.core.Direction;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.phys.Vec3;

public final class ItemFrameRenderHelper {
    private ItemFrameRenderHelper() {}

    private static final double FRAME_FACE_OFFSET = 0.46875D;
    private static final float INVISIBLE_CONTENT_DEPTH = 0.5F;
    private static final float VISIBLE_CONTENT_DEPTH = 0.4375F;
    private static final float ITEM_SCALE = 0.5F;
    private static final float MAP_SCALE = 0.0078125F;
    private static final int MAP_GLOW_LIGHT = LightCoordsUtil.FULL_BRIGHT - ItemFrameRenderer.BRIGHT_MAP_LIGHT_ADJUSTMENT;

    public static Vec3 getRenderOffset(Direction direction) {
        return new Vec3(direction.getStepX() * 0.3F, -0.25D, direction.getStepZ() * 0.3F);
    }

    public static void applyFacingPose(PoseStack poseStack, Direction direction) {
        poseStack.translate(
                direction.getStepX() * FRAME_FACE_OFFSET,
                direction.getStepY() * FRAME_FACE_OFFSET,
                direction.getStepZ() * FRAME_FACE_OFFSET
        );

        float xRot;
        float yRot;

        if (direction.getAxis().isHorizontal()) {
            xRot = 0.0F;
            yRot = 180.0F - direction.toYRot();
        } else {
            xRot = -90.0F * direction.getAxisDirection().getStep();
            yRot = 180.0F;
        }

        poseStack.mulPose(Axis.XP.rotationDegrees(xRot));
        poseStack.mulPose(Axis.YP.rotationDegrees(yRot));
    }

    public static void translateToContentPlane(PoseStack poseStack, boolean invisible) {
        poseStack.translate(0.0F, 0.0F, invisible ? INVISIBLE_CONTENT_DEPTH : VISIBLE_CONTENT_DEPTH);
    }

    public static void applyMapPose(PoseStack poseStack, int rotation) {
        int snappedRotation = rotation % 4 * 2;
        poseStack.mulPose(Axis.ZP.rotationDegrees(snappedRotation * 360.0F / 8.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
        poseStack.scale(MAP_SCALE, MAP_SCALE, MAP_SCALE);
        poseStack.translate(-64.0F, -64.0F, 0.0F);
        poseStack.translate(0.0F, 0.0F, -1.0F);
    }

    public static void applyItemPose(PoseStack poseStack, int rotation) {
        poseStack.mulPose(Axis.ZP.rotationDegrees(rotation * 360.0F / 8.0F));
        poseStack.scale(ITEM_SCALE, ITEM_SCALE, ITEM_SCALE);
    }

    public static int getMapLight(boolean glowFrame, int originalLight) {
        return glowFrame ? MAP_GLOW_LIGHT : originalLight;
    }

    public static int getItemLight(boolean glowFrame, int originalLight) {
        return glowFrame ? LightCoordsUtil.FULL_BRIGHT : originalLight;
    }
}
