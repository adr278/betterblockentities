package betterblockentities.mixin.sodium.pipeline;

/* local */
import betterblockentities.client.chunk.pipeline.BBEBlockRenderer;
import betterblockentities.client.chunk.pipeline.BBEEmitter;
import betterblockentities.client.chunk.translucent_sorting.TranslucentGeometryCollectorExt;
import betterblockentities.client.chunk.util.QuadLighter;
import betterblockentities.mixin.accessors.AbstractBlockRenderContextAccessor;

/* minecraft */
import com.mojang.blaze3d.vulkan.VulkanGpuBuffer;
import net.caffeinemc.mods.sodium.api.util.NormI8;
import net.caffeinemc.mods.sodium.client.model.light.LightMode;
import net.caffeinemc.mods.sodium.client.model.light.data.QuadLightData;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.ChunkBuildBuffers;
import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.QuadSplittingMode;
import net.caffeinemc.mods.sodium.client.render.helper.ColorHelper;
import net.caffeinemc.mods.sodium.client.render.model.SodiumShadeMode;
import net.caffeinemc.mods.sodium.client.services.PlatformBlockAccess;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.ARGB;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;

/* sodium */
import net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderer;
import net.caffeinemc.mods.sodium.client.render.model.MutableQuadViewImpl;
import net.caffeinemc.mods.sodium.client.services.PlatformModelEmitter;
import net.caffeinemc.mods.sodium.client.model.color.ColorProviderRegistry;
import net.caffeinemc.mods.sodium.client.model.light.LightPipelineProvider;
import net.caffeinemc.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.TranslucentGeometryCollector;
import net.caffeinemc.mods.sodium.client.render.chunk.vertex.format.ChunkVertexEncoder;
import net.caffeinemc.mods.sodium.client.world.LevelSlice;

/* mixin */
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

/* java/misc */
import java.util.Arrays;
import java.util.function.Predicate;

@Pseudo
@Mixin(BlockRenderer.class)
public abstract class BlockRendererMixin {
    /* make sodium own this so it lives and dies alongside Sodium's BlockRenderer */
    @Unique private BBEBlockRenderer bbeBlockRenderer;
    @Unique private QuadLighter bbeQuadLighter;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void bbe$init(ColorProviderRegistry colorRegistry, LightPipelineProvider lighters, CallbackInfo ci) {
        this.bbeBlockRenderer = new BBEBlockRenderer((BlockRenderer)(Object)this);
        this.bbeQuadLighter = new QuadLighter();
    }

    @Inject(method = "prepare", at = @At("TAIL"))
    private void bbe$prepare(ChunkBuildBuffers buffers, LevelSlice level, TranslucentGeometryCollector collector, CallbackInfo ci) {
        this.bbeQuadLighter.prepare(level);
    }
    
    @Redirect(
            method = "renderModel(Lnet/minecraft/client/renderer/block/dispatch/BlockStateModel;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/BlockPos;)V",
            at = @At(
                    value = "INVOKE",
                    target = "net/caffeinemc/mods/sodium/client/services/PlatformModelEmitter.emitModel (Lnet/minecraft/client/renderer/block/dispatch/BlockStateModel;Ljava/util/function/Predicate;Lnet/caffeinemc/mods/sodium/client/render/model/MutableQuadViewImpl;Lnet/minecraft/util/RandomSource;Lnet/minecraft/client/renderer/block/BlockAndTintGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/caffeinemc/mods/sodium/client/services/PlatformModelEmitter$Bufferer;)V"
            )
    )
    public void bbe$emitModel(PlatformModelEmitter instance, BlockStateModel model, Predicate<Direction> isFaceCulled, MutableQuadViewImpl emitter, RandomSource random, BlockAndTintGetter level, BlockPos pos, BlockState state, PlatformModelEmitter.Bufferer bufferer) {
        LevelSlice slice = ((AbstractBlockRenderContextAccessor)(Object)this).bbe$getSlice();
        bbeBlockRenderer.emitBlockModel(instance, model, isFaceCulled, emitter, random, level, slice, pos, state, bufferer);
    }

    @WrapOperation(method = "processQuad",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/caffeinemc/mods/sodium/client/render/chunk/compile/pipeline/BlockRenderer;shadeQuad(Lnet/caffeinemc/mods/sodium/client/render/model/MutableQuadViewImpl;Lnet/caffeinemc/mods/sodium/client/model/light/LightMode;ZLnet/caffeinemc/mods/sodium/client/render/model/SodiumShadeMode;)V"
            )
    )
    private void bbe$shadeQuad(BlockRenderer instance, MutableQuadViewImpl quad, LightMode lightMode, boolean emissive, SodiumShadeMode sodiumShadeMode, Operation<Void> original) {
        if ((quad.getTag() & BBEEmitter.IMMEDIATE_SHADING) == 0) {
            original.call(instance, quad, lightMode, emissive, sodiumShadeMode);
            return;
        }

        AbstractBlockRenderContextAccessor acc = (AbstractBlockRenderContextAccessor)(Object)this;

        BlockPos pos = acc.bbe$getPos();
        BlockState state = acc.bbe$getState();
        QuadLightData quadLightData = acc.bbe$quadLightData();

        this.bbeQuadLighter.shadeEntityQuad(pos, state, emissive, quad, quadLightData);
    }

    @WrapOperation(method = "bufferQuad",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/caffeinemc/mods/sodium/client/render/chunk/translucent_sorting/TranslucentGeometryCollector;appendQuad([Lnet/caffeinemc/mods/sodium/client/render/chunk/vertex/format/ChunkVertexEncoder$Vertex;Lnet/caffeinemc/mods/sodium/client/model/quad/properties/ModelQuadFacing;I)Z"
            )
    )
    public boolean bbe$appendQuad(
            TranslucentGeometryCollector instance,
            ChunkVertexEncoder.Vertex[] vertices,
            ModelQuadFacing facing,
            int packedNormal,
            Operation<Boolean> original,
            @Local(ordinal = 0)MutableQuadViewImpl quad
    ) {
        TranslucentGeometryCollectorExt tscExt = (TranslucentGeometryCollectorExt)instance;

        try {
            if ((quad.getTag() & BBEEmitter.NO_QUAD_SPLITTING) != 0) {
                tscExt.bbe$setIncomingQuadSplitMode(QuadSplittingMode.OFF);
            }
            return original.call(instance, vertices, facing, packedNormal);
        } finally {
            tscExt.bbe$deferSplittingMode();
        }
    }
}