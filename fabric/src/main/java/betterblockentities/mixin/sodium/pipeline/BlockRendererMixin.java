package betterblockentities.mixin.sodium.pipeline;

/* local */
import betterblockentities.client.chunk.pipeline.BBEBlockRenderer;
import betterblockentities.client.chunk.pipeline.BBEEmitter;
import betterblockentities.client.chunk.translucent_sorting.TranslucentGeometryCollectorExt;

/* minecraft */
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
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
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

/* java/misc */
import java.util.function.Predicate;

@Pseudo
@Mixin(BlockRenderer.class)
public abstract class BlockRendererMixin {
    /* make sodium own this so it lives and dies alongside Sodium's BlockRenderer */
    @Unique private BBEBlockRenderer bbeBlockRenderer;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void init(ColorProviderRegistry colorRegistry, LightPipelineProvider lighters, CallbackInfo ci) {
        this.bbeBlockRenderer = new BBEBlockRenderer((BlockRenderer)(Object)this);
    }

    @Redirect(method = "renderModel",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/caffeinemc/mods/sodium/client/services/PlatformModelEmitter;emitModel(Lnet/minecraft/client/renderer/block/model/BlockStateModel;Ljava/util/function/Predicate;Lnet/caffeinemc/mods/sodium/client/render/model/MutableQuadViewImpl;Lnet/minecraft/util/RandomSource;Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/caffeinemc/mods/sodium/client/services/PlatformModelEmitter$Bufferer;)V"
            )
    )
    public void emitModel(PlatformModelEmitter instance, BlockStateModel model, Predicate<Direction> isFaceCulled, MutableQuadViewImpl emitter, RandomSource random, BlockAndTintGetter level, BlockPos pos, BlockState state, PlatformModelEmitter.Bufferer bufferer) {
        LevelSlice slice = ((AbstractBlockRenderContextAccessor)(Object)this).getSlice();
        bbeBlockRenderer.emitBlockModel(instance, model, isFaceCulled, emitter, random, level, slice, pos, state, bufferer);
    }

    @WrapOperation(method = "bufferQuad",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/caffeinemc/mods/sodium/client/render/chunk/translucent_sorting/TranslucentGeometryCollector;appendQuad([Lnet/caffeinemc/mods/sodium/client/render/chunk/vertex/format/ChunkVertexEncoder$Vertex;Lnet/caffeinemc/mods/sodium/client/model/quad/properties/ModelQuadFacing;I)Z"
            )
    )
    public boolean appendQuad(
            TranslucentGeometryCollector instance,
            ChunkVertexEncoder.Vertex[] vertices,
            ModelQuadFacing facing,
            int packedNormal,
            Operation<Boolean> original,
            @Local(ordinal = 0)MutableQuadViewImpl quad
    ) {
        TranslucentGeometryCollectorExt tscExt = (TranslucentGeometryCollectorExt)instance;

        try {
            if (quad.getTag() == BBEEmitter.NO_QUAD_SPLITTING) {
                tscExt.setIncomingQuadSplitMode(BBEEmitter.QuadSplittingMode.NONE);
            }
            return original.call(instance, vertices, facing, packedNormal);
        } finally {
            tscExt.deferSplittingMode();
        }
    }
}