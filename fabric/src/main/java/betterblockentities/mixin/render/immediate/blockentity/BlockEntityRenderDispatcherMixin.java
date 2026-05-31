package betterblockentities.mixin.render.immediate.blockentity;

/* local */
import betterblockentities.client.BBE;
import betterblockentities.client.gui.config.BBEConfig;
import betterblockentities.client.render.immediate.blockentity.misc.CrumblingOverlayConsumer;
import betterblockentities.client.render.immediate.blockentity.extentions.BlockEntityExt;
import betterblockentities.client.render.immediate.blockentity.manager.InstancedBlockEntityManager;
import betterblockentities.client.render.immediate.blockentity.manager.SpecialBlockEntityManager;
import betterblockentities.client.render.immediate.blockentity.misc.CrumblingRenderContext;
import betterblockentities.client.render.immediate.blockentity.misc.RenderingMode;
import betterblockentities.render.AltBlockEntityRenderState;
import betterblockentities.render.AltRenderers;

/* minecraft */
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

/* mojang */
import com.mojang.blaze3d.vertex.PoseStack;

/* mixin */
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/* java/misc */
import java.util.List;

@Mixin(BlockEntityRenderDispatcher.class)
public abstract class BlockEntityRenderDispatcherMixin {
    @Shadow public Camera camera;

    @WrapOperation(
            method = "renderItem",
            at = @At(
                    value = "INVOKE",
                    target = "net/minecraft/client/renderer/blockentity/BlockEntityRenderDispatcher.tryRender (Lnet/minecraft/world/level/block/entity/BlockEntity;Ljava/lang/Runnable;)V",
                    ordinal = 0
            )
    )
    public <E extends BlockEntity> void detectItemInvokedRenderers(BlockEntity blockEntity, Runnable runnable, Operation<Void> original) {
        BBE.GlobalScope.isItemInvoked = true;
        original.call(blockEntity, runnable);
        BBE.GlobalScope.isItemInvoked = false;
    }

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void manageBlockEntities(
            final BlockEntity blockEntity,
            final float partialTick,
            final PoseStack poseStack,
            final MultiBufferSource vertexConsumers,
            final CallbackInfo ci
    ) {
        final BlockEntityExt ext = (BlockEntityExt) blockEntity;

        BBE.GlobalScope.limitVanillaSignRendering = false;

        if (!shouldManage(ext)) {
            return;
        }

        final boolean renderSpecialsInImmediate = shouldRenderSpecialsInImmediate(blockEntity, ext);
        final boolean crumblingPass = isCrumblingPass(vertexConsumers);

        BBE.GlobalScope.limitVanillaSignRendering = renderSpecialsInImmediate && !crumblingPass;

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

    @Unique private static boolean isCrumblingPass(final MultiBufferSource vertexConsumers) {
        if (CrumblingRenderContext.isActive()) {
            return true;
        }

        return !(vertexConsumers instanceof MultiBufferSource.BufferSource);
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

    @Unique private static boolean shouldManage(final BlockEntityExt ext) {
        return BBEConfig.OptEnabledTable.ENABLED[ext.optKind() & 0xFF]
                && ext.terrainMeshReady()
                && ext.renderingMode() == RenderingMode.TERRAIN;
    }

    @Unique private static boolean shouldRenderSpecialsInImmediate(
            final BlockEntity blockEntity,
            final BlockEntityExt ext
    ) {
        if (!ext.hasSpecialManager()) {
            return false;
        }

        return SpecialBlockEntityManager.shouldRender(blockEntity);
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
                new CrumblingOverlayConsumer.CrumblingOnlyBufferSource(vertexConsumers),
                light,
                OverlayTexture.NO_OVERLAY
        );
    }


}
