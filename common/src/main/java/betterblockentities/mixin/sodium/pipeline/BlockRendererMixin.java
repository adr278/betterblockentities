package betterblockentities.mixin.sodium.pipeline;

/* local */
import betterblockentities.client.chunk.pipeline.BBEEmitter;
import betterblockentities.client.chunk.translucent_sorting.TranslucentGeometryCollectorExt;
import betterblockentities.client.chunk.pipeline.BBEBlockRenderer;
import betterblockentities.client.chunk.util.QuadLighter;

/* fabric */
import net.fabricmc.fabric.api.renderer.v1.model.FabricBakedModel;
import net.fabricmc.fabric.api.renderer.v1.material.ShadeMode;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;

/* minecraft */
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;

/* sodium */
import net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderer;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.ChunkBuildBuffers;
import net.caffeinemc.mods.sodium.client.render.frapi.mesh.MutableQuadViewImpl;
import net.caffeinemc.mods.sodium.client.render.frapi.render.AbstractBlockRenderContext;
import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.TranslucentGeometryCollector;
import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.QuadSplittingMode;
import net.caffeinemc.mods.sodium.client.render.chunk.vertex.format.ChunkVertexEncoder;
import net.caffeinemc.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import net.caffeinemc.mods.sodium.client.world.LevelSlice;
import net.caffeinemc.mods.sodium.client.model.color.ColorProviderRegistry;
import net.caffeinemc.mods.sodium.client.model.light.LightPipelineProvider;
import net.caffeinemc.mods.sodium.client.model.light.LightMode;

/* mixin */
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/* java/misc */
import java.util.function.Supplier;

@Pseudo
@Mixin(BlockRenderer.class)
public abstract class BlockRendererMixin {
    /* make sodium own this so it lives and dies alongside Sodium's BlockRenderer */
    @Unique private BBEBlockRenderer bbeBlockRenderer;
    @Unique private QuadLighter bbeQuadLighter;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void init(ColorProviderRegistry colorRegistry, LightPipelineProvider lighters, CallbackInfo ci) {
        bbeBlockRenderer = new BBEBlockRenderer();
        bbeQuadLighter = new QuadLighter();
    }

    @Inject(method = "prepare", at = @At("TAIL"))
    private void prepare(ChunkBuildBuffers buffers, LevelSlice level, TranslucentGeometryCollector collector, CallbackInfo ci) {
        this.bbeQuadLighter.prepare(level);
    }

    @WrapOperation(
            method = "renderModel",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/fabricmc/fabric/api/renderer/v1/model/FabricBakedModel;emitBlockQuads(Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Ljava/util/function/Supplier;Lnet/fabricmc/fabric/api/renderer/v1/render/RenderContext;)V"
            )
    )
    private void emitTerrainBEs(
            FabricBakedModel instance,
            BlockAndTintGetter level,
            BlockState state,
            BlockPos pos,
            Supplier<RandomSource> randomSupplier,
            RenderContext context,
            Operation<Void> original
    ) {
        original.call(instance, level, state, pos, randomSupplier, context);

        LevelSlice slice = ((AbstractBlockRenderContextAccessor)(Object)this).getSlice();
        MutableQuadViewImpl sodiumEmitter = (MutableQuadViewImpl)((AbstractBlockRenderContext)(Object)this).getEmitter();

        bbeBlockRenderer.emitBlockModel(sodiumEmitter, slice, pos, state, randomSupplier);
    }

    @WrapOperation(
            method = "processQuad",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/caffeinemc/mods/sodium/client/render/chunk/compile/pipeline/BlockRenderer;shadeQuad(Lnet/caffeinemc/mods/sodium/client/render/frapi/mesh/MutableQuadViewImpl;Lnet/caffeinemc/mods/sodium/client/model/light/LightMode;ZLnet/fabricmc/fabric/api/renderer/v1/material/ShadeMode;)V"
            )
    )
    private void shadeQuad(BlockRenderer instance, MutableQuadViewImpl quad, LightMode lightMode, boolean emissive, ShadeMode shadeMode, Operation<Void> original) {
        if ((quad.tag() & BBEEmitter.IMMEDIATE_SHADING) == 0) {
            original.call(instance, quad, lightMode, emissive, shadeMode);
            return;
        }

        AbstractBlockRenderContextAccessor context = (AbstractBlockRenderContextAccessor)(Object)this;
        this.bbeQuadLighter.shadeEntityQuad(
                context.getPos(),
                context.getState(),
                emissive,
                quad,
                context.getQuadLightData()
        );
    }

    @WrapOperation(method = "bufferQuad",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/caffeinemc/mods/sodium/client/render/chunk/translucent_sorting/TranslucentGeometryCollector;appendQuad([Lnet/caffeinemc/mods/sodium/client/render/chunk/vertex/format/ChunkVertexEncoder$Vertex;Lnet/caffeinemc/mods/sodium/client/model/quad/properties/ModelQuadFacing;I)Z"
            )
    )
    private boolean appendQuad(
            TranslucentGeometryCollector collector,
            ChunkVertexEncoder.Vertex[] vertices,
            ModelQuadFacing facing,
            int packedNormal,
            Operation<Boolean> original,
            @Local(ordinal = 0) MutableQuadViewImpl quad
    ) {
        final TranslucentGeometryCollectorExt collectorExt = (TranslucentGeometryCollectorExt) collector;

        try {
            if ((quad.tag() & BBEEmitter.NO_QUAD_SPLITTING) != 0) {
                collectorExt.setIncomingQuadSplitMode(QuadSplittingMode.OFF);
            }
            return original.call(collector, vertices, facing, packedNormal);
        } finally {
            collectorExt.deferSplittingMode();
        }
    }
}