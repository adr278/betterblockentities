package betterblockentities.mixin.sodium.render;

/* local */
import betterblockentities.client.gui.config.BBEConfig;
import betterblockentities.platform.GlobalScope;
import betterblockentities.render.AltRenderers;
import betterblockentities.client.render.immediate.blockentity.extentions.BlockEntityExt;
import betterblockentities.client.render.immediate.blockentity.manager.SpecialBlockEntityManager;

/* minecraft */
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.BlockDestructionProgress;
import net.minecraft.world.level.block.entity.*;

/* mojang */
import com.mojang.blaze3d.vertex.PoseStack;

/* sodium */
import net.caffeinemc.mods.sodium.client.render.SodiumWorldRenderer;

/* mixin */
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;

/* java/misc */
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import java.util.List;
import java.util.SortedSet;

@Pseudo
@Mixin(SodiumWorldRenderer.class)
public abstract class SodiumWorldRendererMixin {
    /**
     * @author ceeden
     * @reason We overwrite this because we don't want other mods in here, this is a critical mixin that
     * can mess a lot of stuff up if other mods change execution flow. If additional renders needs to be ran or
     * something similar, our API is available for just that
     */
    @Overwrite
    private void extractBlockEntity(BlockEntity blockEntity, PoseStack poseStack, Camera camera, float tickDelta, Long2ObjectMap<SortedSet<BlockDestructionProgress>> progression, LevelRenderState levelRenderState, boolean globalBlockEntity) {
        final BlockPos blockPos = blockEntity.getBlockPos();
        final SortedSet<BlockDestructionProgress> sortedSet = progression.get(blockPos.asLong());

        ModelFeatureRenderer.CrumblingOverlay crumblingOverlay;
        if (sortedSet != null && !sortedSet.isEmpty()) {
            poseStack.pushPose();
            poseStack.translate(
                    (double) blockPos.getX() - camera.position().x,
                    (double) blockPos.getY() - camera.position().y,
                    (double) blockPos.getZ() - camera.position().z
            );
            crumblingOverlay = new ModelFeatureRenderer.CrumblingOverlay(sortedSet.last().getProgress(), poseStack.last());
            poseStack.popPose();
        } else {
            crumblingOverlay = null;
        }

        /* extract our registered alt renderers for this block entity */
        if (AltRenderers.renderersLoaded()) {
            List<BlockEntityRenderState> altBlockEntityRenderStates =
                    GlobalScope.altRenderDispatcher.tryExtractRenderStates(blockEntity, tickDelta, crumblingOverlay);
            for (BlockEntityRenderState altState : altBlockEntityRenderStates) {
                if (altState != null) {
                    GlobalScope.altBlockEntityRenderStates.add(altState);
                }
            }
        }

        /* manage this block entity if optimizations for it is turned on */
        BlockEntityExt ext = (BlockEntityExt)blockEntity;
        if (this.bbe$shouldManage(ext, crumblingOverlay)) {
            if (ext.bbe$hasSpecialManager()) {
                BlockEntityRenderState managedState =
                        SpecialBlockEntityManager.extractManagedState(blockEntity, levelRenderState.cameraRenderState, tickDelta, crumblingOverlay, globalBlockEntity);

                if (managedState != null) {
                    levelRenderState.blockEntityRenderStates.add(managedState);
                }
            }
            return;
        }

        /* extract the default registered render state */
        BlockEntityRenderState blockEntityRenderState =
                Minecraft.getInstance().getBlockEntityRenderDispatcher().tryExtractRenderState(blockEntity, tickDelta, crumblingOverlay, globalBlockEntity);
        if (blockEntityRenderState != null) {
            levelRenderState.blockEntityRenderStates.add(blockEntityRenderState);
        }
    }

    @Unique
    private boolean bbe$shouldManage(BlockEntityExt ext, ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
        return ext.bbe$isSupportedBlockEntity()                               &&
                BBEConfig.OptEnabledTable.ENABLED[ext.bbe$getOptKind() & 0xFF] &&
                ext.bbe$isTerrainMeshReady()                                  &&
                crumblingOverlay == null;
    }
}