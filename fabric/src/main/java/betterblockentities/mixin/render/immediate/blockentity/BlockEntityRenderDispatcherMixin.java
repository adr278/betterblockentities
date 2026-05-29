package betterblockentities.mixin.render.immediate.blockentity;

/* local */
import betterblockentities.client.BBE;
import betterblockentities.client.gui.config.BBEConfig;
import betterblockentities.client.render.immediate.blockentity.extentions.BlockEntityExt;
import betterblockentities.client.render.immediate.blockentity.manager.InstancedBlockEntityManager;
import betterblockentities.client.render.immediate.blockentity.manager.SpecialBlockEntityManager;
import betterblockentities.client.render.immediate.blockentity.misc.CrumblingRenderContext;
import betterblockentities.client.render.immediate.blockentity.misc.RenderingMode;
import betterblockentities.render.AltBlockEntityRenderState;
import betterblockentities.render.AltRenderers;

/* minecraft */
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

/* mojang */
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

/* mixin */
import org.jspecify.annotations.NonNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/* java/misc */
import java.util.List;

@Mixin(BlockEntityRenderDispatcher.class)
public abstract class BlockEntityRenderDispatcherMixin {
    @Shadow private Camera camera;

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void cancelTerrainReadyBlockEntities(
            final BlockEntity blockEntity,
            final float partialTick,
            final PoseStack poseStack,
            final MultiBufferSource vertexConsumers,
            final CallbackInfo ci
    ) {
        final BlockEntityExt ext = (BlockEntityExt) blockEntity;
        final byte optKind = resolveOptKind(blockEntity, ext);
        BBE.GlobalScope.limitVanillaSignRendering = false;

        if (!shouldUseTerrainPath(ext, optKind)) {
            return;
        }

        final boolean renderSpecialsInImmediate = shouldRenderSpecialsInImmediate(blockEntity, ext, optKind);
        final boolean crumblingPass = isCrumblingPass(vertexConsumers);

        if (optKind == InstancedBlockEntityManager.OptKind.SIGN) {
            BBE.GlobalScope.limitVanillaSignRendering = renderSpecialsInImmediate && !crumblingPass;
        }

        if (crumblingPass) {
            if (!renderSpecialsInImmediate) {
                renderCrumblingOnly(blockEntity, partialTick, poseStack, vertexConsumers);
                ci.cancel();
            }
            return;
        }

        if (!renderSpecialsInImmediate) {
            ci.cancel();
        }
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void resetSignLimitFlag(
            final BlockEntity blockEntity,
            final float partialTick,
            final PoseStack poseStack,
            final MultiBufferSource vertexConsumers,
            final CallbackInfo ci
    ) {
        BBE.GlobalScope.limitVanillaSignRendering = false;
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void submitAltRenderers(
            final BlockEntity blockEntity,
            final float partialTick,
            final PoseStack poseStack,
            final MultiBufferSource vertexConsumers,
            final CallbackInfo ci
    ) {
        if (!AltRenderers.renderersLoaded() || BBE.GlobalScope.altRenderDispatcher == null || this.camera == null) {
            return;
        }

        BBE.GlobalScope.altRenderDispatcher.prepare(this.camera.getPosition());

        List<AltBlockEntityRenderState> states =
                BBE.GlobalScope.altRenderDispatcher.tryExtractRenderStates(blockEntity, partialTick, null);

        if (states.isEmpty()) {
            return;
        }

        assert blockEntity.getLevel() != null;
        int light = LevelRenderer.getLightColor(blockEntity.getLevel(), blockEntity.getBlockPos());
        for (AltBlockEntityRenderState state : states) {
            BBE.GlobalScope.altRenderDispatcher.submit(
                    state,
                    poseStack,
                    vertexConsumers,
                    this.camera,
                    light,
                    OverlayTexture.NO_OVERLAY
            );
        }

        BBE.GlobalScope.altRenderDispatcher.clearStateRendererPairs();
    }

    @Unique private static boolean shouldUseTerrainPath(final BlockEntityExt ext, final byte optKind) {
        return optKind != InstancedBlockEntityManager.OptKind.NONE
                && BBEConfig.OptEnabledTable.ENABLED[optKind & 0xFF]
                && ext.terrainMeshReady()
                && ext.renderingMode() == RenderingMode.TERRAIN;
    }

    @Unique private static boolean shouldRenderSpecialsInImmediate(
            final BlockEntity blockEntity,
            final BlockEntityExt ext,
            final byte optKind
    ) {
        if (!ext.hasSpecialManager()) {
            return false;
        }

        if (optKind != InstancedBlockEntityManager.OptKind.CAMPFIRE
                && optKind != InstancedBlockEntityManager.OptKind.SIGN) {
            return false;
        }

        return SpecialBlockEntityManager.shouldRender(blockEntity);
    }

    @Unique private static boolean isCrumblingPass(final MultiBufferSource vertexConsumers) {
        if (CrumblingRenderContext.isActive()) {
            return true;
        }

        return !(vertexConsumers instanceof MultiBufferSource.BufferSource);
    }

    @Unique private static byte resolveOptKind(final BlockEntity blockEntity, final BlockEntityExt ext) {
        final byte existing = ext.optKind();
        if (existing != InstancedBlockEntityManager.OptKind.NONE) {
            return existing;
        }

        final BlockEntityType<?> type = blockEntity.getType();

        if (type == BlockEntityType.CHEST || type == BlockEntityType.TRAPPED_CHEST || type == BlockEntityType.ENDER_CHEST) {
            return InstancedBlockEntityManager.OptKind.CHEST;
        }
        if (type == BlockEntityType.SIGN || type == BlockEntityType.HANGING_SIGN) {
            return InstancedBlockEntityManager.OptKind.SIGN;
        }
        if (type == BlockEntityType.BED) {
            return InstancedBlockEntityManager.OptKind.BED;
        }
        if (type == BlockEntityType.SHULKER_BOX) {
            return InstancedBlockEntityManager.OptKind.SHULKER;
        }
        if (type == BlockEntityType.DECORATED_POT) {
            return InstancedBlockEntityManager.OptKind.POT;
        }
        if (type == BlockEntityType.BANNER) {
            return InstancedBlockEntityManager.OptKind.BANNER;
        }
        if (type == BlockEntityType.BELL) {
            return InstancedBlockEntityManager.OptKind.BELL;
        }
        if (type == BlockEntityType.CAMPFIRE) {
            return InstancedBlockEntityManager.OptKind.CAMPFIRE;
        }

        return InstancedBlockEntityManager.OptKind.NONE;
    }

    @Unique private void renderCrumblingOnly(
            final BlockEntity blockEntity,
            final float partialTick,
            final PoseStack poseStack,
            final MultiBufferSource vertexConsumers
    ) {
        if (this.camera == null || !blockEntity.hasLevel() || blockEntity.getLevel() == null) {
            return;
        }
        if (!blockEntity.getType().isValid(blockEntity.getBlockState())) {
            return;
        }

        final BlockEntityRenderDispatcher dispatcher = (BlockEntityRenderDispatcher) (Object) this;

        final BlockEntityRenderer<BlockEntity> renderer = (BlockEntityRenderer<BlockEntity>) dispatcher.getRenderer(blockEntity);
        if (renderer == null || !renderer.shouldRender(blockEntity, this.camera.getPosition())) {
            return;
        }

        final int light = LevelRenderer.getLightColor(blockEntity.getLevel(), blockEntity.getBlockPos());

        renderer.render(
                blockEntity,
                partialTick,
                poseStack,
                new CrumblingOnlyBufferSource(vertexConsumers),
                light,
                OverlayTexture.NO_OVERLAY
        );
    }

    @Unique private record CrumblingOnlyBufferSource(MultiBufferSource delegate) implements MultiBufferSource {

        @Override public @NonNull VertexConsumer getBuffer(final RenderType renderType) {
                if (!renderType.affectsCrumbling()) {
                    return NoopVertexConsumer.INSTANCE;
                }

                final VertexConsumer vertexConsumer = this.delegate.getBuffer(renderType);
                if (vertexConsumer instanceof VertexMultiConsumerDoubleAccessor doubleConsumer) {
                    return doubleConsumer.getFirst();
                }

                return vertexConsumer;
            }
        }

    @Unique private enum NoopVertexConsumer implements VertexConsumer {
        INSTANCE;

        @Override public @NonNull VertexConsumer addVertex(final float x, final float y, final float z) {
            return this;
        }

        @Override public @NonNull VertexConsumer setColor(final int red, final int green, final int blue, final int alpha) {
            return this;
        }

        @Override public @NonNull VertexConsumer setUv(final float u, final float v) {
            return this;
        }

        @Override public @NonNull VertexConsumer setUv1(final int u, final int v) {
            return this;
        }

        @Override public @NonNull VertexConsumer setUv2(final int u, final int v) {
            return this;
        }

        @Override public @NonNull VertexConsumer setNormal(final float normalX, final float normalY, final float normalZ) {
            return this;
        }
    }
}
