package betterblockentities.mixin.sodium.render;

/* local */
import betterblockentities.client.gui.config.BBEConfig;
import betterblockentities.client.render.immediate.blockentity.extentions.BlockEntityExt;
import betterblockentities.client.render.immediate.blockentity.manager.SpecialBlockEntityManager;
import betterblockentities.client.render.immediate.blockentity.misc.CrumblingRenderContext;
import betterblockentities.render.AltRenderers;

/* minecraft */
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.server.level.BlockDestructionProgress;
import net.minecraft.world.level.block.entity.BlockEntity;

/* mojang */
import com.mojang.blaze3d.vertex.PoseStack;

/* sodium */
import net.caffeinemc.mods.sodium.client.render.SodiumWorldRenderer;

/* mixin */
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

/* mixin extras */
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;

/* java/misc */
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import java.util.SortedSet;

/**
 * Skips redundant immediate renders and tells the dispatcher when Sodium has supplied a breaking overlay.
 */
@Pseudo
@Mixin(SodiumWorldRenderer.class)
public abstract class SodiumBlockEntityRenderContextMixin {
    /**
     * Nvidium replaces Sodium's block entity loop, but still delegates each entry to this helper.
     */
    @WrapMethod(method = "renderBlockEntity")
    private static void skipRedundantImmediateRender(
            PoseStack matrices,
            RenderBuffers bufferBuilders,
            Long2ObjectMap<SortedSet<BlockDestructionProgress>> blockBreakingProgressions,
            float tickDelta,
            MultiBufferSource.BufferSource immediate,
            double x,
            double y,
            double z,
            BlockEntityRenderDispatcher dispatcher,
            BlockEntity entity,
            LocalPlayer player,
            LocalBooleanRef isGlowing,
            Operation<Void> original
    ) {
        if (canSkipImmediateRender(entity, blockBreakingProgressions)) {
            return;
        }

        original.call(
                matrices,
                bufferBuilders,
                blockBreakingProgressions,
                tickDelta,
                immediate,
                x,
                y,
                z,
                dispatcher,
                entity,
                player,
                isGlowing
        );
    }

    @WrapOperation(
            method = "renderBlockEntity",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/blockentity/BlockEntityRenderDispatcher;render(Lnet/minecraft/world/level/block/entity/BlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;)V"
            )
    )
    private static void renderWithCrumblingContext(
            BlockEntityRenderDispatcher dispatcher,
            BlockEntity blockEntity,
            float f,
            PoseStack poseStack,
            MultiBufferSource multiBufferSource,
            Operation<Void> original,
            @Local(argsOnly = true) MultiBufferSource.BufferSource immediate
    ) {
        if (multiBufferSource == immediate) {
            original.call(dispatcher, blockEntity, f, poseStack, multiBufferSource);
            return;
        }

        CrumblingRenderContext.push();
        try {
            original.call(dispatcher, blockEntity, f, poseStack, multiBufferSource);
        } finally {
            CrumblingRenderContext.pop();
        }
    }

    @Unique
    private static boolean canSkipImmediateRender(
            BlockEntity blockEntity,
            Long2ObjectMap<SortedSet<BlockDestructionProgress>> blockBreakingProgressions
    ) {
        BlockEntityExt ext = (BlockEntityExt)blockEntity;
        if (!ext.terrainRenderingReady()
                || !BBEConfig.OptEnabledTable.ENABLED[ext.optKind() & 0xFF]) {
            return false;
        }

        if (AltRenderers.renderersLoaded()) {
            return false;
        }

        boolean dispatcherWillCancel = !ext.hasSpecialManager()
                || !SpecialBlockEntityManager.shouldRender(blockEntity);
        if (!dispatcherWillCancel) {
            return false;
        }

        if (blockBreakingProgressions.isEmpty()) {
            return true;
        }

        SortedSet<BlockDestructionProgress> breakingProgression = blockBreakingProgressions.get(
                blockEntity.getBlockPos().asLong()
        );
        return breakingProgression == null
                || breakingProgression.isEmpty()
                || breakingProgression.last().getProgress() < 0;
    }
}
