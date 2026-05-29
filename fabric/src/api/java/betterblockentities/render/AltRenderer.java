package betterblockentities.render;

/* minecraft */
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.server.level.BlockDestructionProgress;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

/* java/misc */
import com.mojang.blaze3d.vertex.PoseStack;
import org.jetbrains.annotations.Nullable;

public interface AltRenderer<T extends BlockEntity, S extends AltBlockEntityRenderState> {
    S createRenderState();

    default void extractRenderState(final T blockEntity, final S state, final float partialTicks, final Vec3 cameraPosition, final @Nullable BlockDestructionProgress breakProgress) {
        AltBlockEntityRenderState.extractBase(blockEntity, state);
    }

    void submit(final S state, final PoseStack poseStack, final MultiBufferSource vertexConsumers, final Camera camera, final int light, final int overlay);

    default int getViewDistance() {
        return 64;
    }

    default boolean shouldRender(final T blockEntity, final Vec3 cameraPosition) {
        return Vec3.atCenterOf(blockEntity.getBlockPos()).closerThan(cameraPosition, this.getViewDistance());
    }

    default boolean dedicatedRenderer() {
        return false;
    }
}
