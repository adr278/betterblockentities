package betterblockentities.mixin.sodium.render;

/* local */
import betterblockentities.client.gui.config.BBEConfig;
import betterblockentities.client.render.immediate.blockentity.extentions.BlockEntityExt;
import betterblockentities.client.render.immediate.blockentity.manager.SpecialBlockEntityManager;
import betterblockentities.client.render.immediate.blockentity.misc.CrumblingOverlayConsumer;
import betterblockentities.client.render.immediate.blockentity.misc.RenderingMode;
import betterblockentities.platform.GlobalScope;
import betterblockentities.render.AltRenderers;

/* minecraft */
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.BlockDestructionProgress;
import net.minecraft.world.level.block.entity.BlockEntity;

/* mojang */
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.SheetedDecalTextureGenerator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexMultiConsumer;

/* sodium */
import net.caffeinemc.mods.sodium.client.render.SodiumWorldRenderer;
import net.caffeinemc.mods.sodium.client.services.PlatformBlockAccess;

/* mixin */
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;

/* java/misc */
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import java.util.SortedSet;

@Mixin(SodiumWorldRenderer.class)
public class SodiumWorldRendererMixin {
    /**
     * @author ceeden
     * @reason We overwrite this because we don't want other mods in here, this is a critical mixin that
     * can mess a lot of stuff up if other mods change execution flow. If additional renders needs to be ran or
     * something similar, our API is available for just that
     */
    @Overwrite
    private static void renderBlockEntity(PoseStack matrices, RenderBuffers bufferBuilders, Long2ObjectMap<SortedSet<BlockDestructionProgress>> blockBreakingProgressions, float tickDelta, MultiBufferSource.BufferSource immediate, double x, double y, double z, BlockEntityRenderDispatcher dispatcher, BlockEntity entity, LocalPlayer player, LocalBooleanRef isGlowing) {
        BlockEntityExt ext = (BlockEntityExt) entity;
        boolean managed = shouldManage(ext);
        boolean skipVanillaRender = managed && (!ext.hasSpecialManager() || !SpecialBlockEntityManager.shouldRender(entity));
        boolean hasAltRenderers = AltRenderers.renderersLoaded();

        if (skipVanillaRender && blockBreakingProgressions.isEmpty() && !hasAltRenderers) {
            return;
        }

        BlockPos pos = entity.getBlockPos();
        SortedSet<BlockDestructionProgress> breakingInfo = blockBreakingProgressions.isEmpty() ? null : blockBreakingProgressions.get(pos.asLong());

        if (skipVanillaRender && breakingInfo == null && !hasAltRenderers) {
            return;
        }

        matrices.pushPose();

        try {
            matrices.translate(
                    pos.getX() - x,
                    pos.getY() - y,
                    pos.getZ() - z
            );

            VertexConsumer crumblingConsumer = null;
            MultiBufferSource consumer = immediate;

            if (breakingInfo != null && !breakingInfo.isEmpty()) {
                int stage = breakingInfo.last().getProgress();
                if (stage >= 0) {
                    VertexConsumer destroyBuffer = bufferBuilders.crumblingBufferSource().getBuffer(ModelBakery.DESTROY_TYPES.get(stage));

                    crumblingConsumer = new SheetedDecalTextureGenerator(destroyBuffer, matrices.last(), 1.0F);
                    VertexConsumer finalCrumblingConsumer = crumblingConsumer;

                    consumer = layer -> layer.affectsCrumbling()
                            ? VertexMultiConsumer.create(finalCrumblingConsumer, immediate.getBuffer(layer))
                            : immediate.getBuffer(layer);
                }
            }

            if (hasAltRenderers) {
                GlobalScope.altRenderDispatcher.render(entity, tickDelta, matrices, consumer);
            }

            if (managed) {
                if (breakingInfo != null) {
                    dispatcher.render(entity, tickDelta, matrices, new CrumblingOverlayConsumer.CrumblingOnlyBufferSource(immediate, crumblingConsumer));
                }

                if (skipVanillaRender) {
                    return;
                }
            }

            GlobalScope.limitVanillaSignRendering = true;
            try {
                dispatcher.render(entity, tickDelta, matrices, consumer);
            } finally {
                GlobalScope.limitVanillaSignRendering = false;
            }

            if (isGlowing != null && PlatformBlockAccess.getInstance().shouldBlockEntityGlow(entity, player)) {
                isGlowing.set(true);
            }
        } finally {
            matrices.popPose();
        }
    }

    @Unique
    private static boolean shouldManage(BlockEntityExt ext) {
        return ext.supportedBlockEntity()
                && BBEConfig.OptEnabledTable.ENABLED[ext.optKind() & 0xFF]
                && ext.terrainMeshReady()
                && ext.renderingMode() == RenderingMode.TERRAIN;
    }
}
