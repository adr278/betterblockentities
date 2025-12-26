package betterblockentities.mixin.sodium;

/* local */
import betterblockentities.chunk.BBEDefaultTerrainRenderPasses;
import betterblockentities.util.BlockEntityManager;

/* minecraft */
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.chunk.ChunkSectionLayerGroup;
import net.minecraft.client.renderer.state.LevelRenderState;
import net.minecraft.server.level.BlockDestructionProgress;
import net.minecraft.world.level.block.entity.BlockEntity;

/* mojang */
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.textures.GpuSampler;

/* sodium */
import net.caffeinemc.mods.sodium.client.render.SodiumWorldRenderer;
import net.caffeinemc.mods.sodium.client.render.chunk.ChunkRenderMatrices;
import net.caffeinemc.mods.sodium.client.render.chunk.RenderSectionManager;
import net.caffeinemc.mods.sodium.client.util.FogParameters;

/* mixin */
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/* java/misc */
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import java.util.SortedSet;

@Pseudo
@Mixin(SodiumWorldRenderer.class)
public abstract class SodiumWorldRendererMixin {
    @Shadow private RenderSectionManager renderSectionManager;
    @Shadow private FogParameters lastFogParameters;

    @Inject(method = "extractBlockEntity", at = @At("HEAD"), cancellable = true)
    private void extractBlockEntity(BlockEntity blockEntity, PoseStack poseStack, Camera camera, float tickDelta, Long2ObjectMap<SortedSet<BlockDestructionProgress>> progression, LevelRenderState levelRenderState, CallbackInfo ci) {
        if (!BlockEntityManager.shouldRender(blockEntity)) ci.cancel();
    }

    /* render our custom passes */
    @Inject(method = "drawChunkLayer", at = @At("TAIL"))
    private void drawBBELayers(ChunkSectionLayerGroup group, ChunkRenderMatrices matrices, double x, double y, double z, GpuSampler terrainSampler, CallbackInfo ci) {
        if (group == ChunkSectionLayerGroup.OPAQUE) {
            this.renderSectionManager.renderLayer(matrices, BBEDefaultTerrainRenderPasses.SOLID, x, y, z, this.lastFogParameters, terrainSampler);
            this.renderSectionManager.renderLayer(matrices, BBEDefaultTerrainRenderPasses.CUTOUT, x, y, z, this.lastFogParameters, terrainSampler);
        } else if (group == ChunkSectionLayerGroup.TRANSLUCENT) {
            this.renderSectionManager.renderLayer(matrices, BBEDefaultTerrainRenderPasses.TRANSLUCENT, x, y, z, this.lastFogParameters, terrainSampler);
        }
    }
}