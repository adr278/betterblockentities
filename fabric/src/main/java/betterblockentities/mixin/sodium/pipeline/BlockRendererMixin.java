package betterblockentities.mixin.sodium.pipeline;

/* local */
import betterblockentities.client.gui.config.ConfigCache;
import betterblockentities.client.chunk.translucent_sorting.QuadSplittingMode;
import betterblockentities.client.chunk.translucent_sorting.TranslucentGeometryCollectorExt;
import betterblockentities.client.chunk.pipeline.BBEBlockRenderer;
import betterblockentities.render.AltRenderers;

/* fabric */
import net.fabricmc.fabric.api.renderer.v1.model.FabricBakedModel;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;

/* minecraft */
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.DecoratedPotBlock;
import net.minecraft.world.level.block.EnderChestBlock;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.BannerBlock;
import net.minecraft.world.level.block.WallBannerBlock;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.level.block.CeilingHangingSignBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.WallHangingSignBlock;
import net.minecraft.world.level.block.WallSignBlock;

/* sodium */
import net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderer;
import net.caffeinemc.mods.sodium.client.render.frapi.mesh.MutableQuadViewImpl;
import net.caffeinemc.mods.sodium.client.render.frapi.render.AbstractBlockRenderContext;
import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.TranslucentGeometryCollector;
import net.caffeinemc.mods.sodium.client.render.chunk.vertex.format.ChunkVertexEncoder;
import net.caffeinemc.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import net.caffeinemc.mods.sodium.client.world.LevelSlice;

/* mixin */
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

/* java/misc */
import java.util.function.Supplier;

@Pseudo
@Mixin(BlockRenderer.class)
public abstract class BlockRendererMixin {
    /* make sodium own this so it lives and dies alongside Sodium's BlockRenderer */
    @Unique private static final ThreadLocal<BBEBlockRenderer> TERRAIN_RENDERER = ThreadLocal.withInitial(BBEBlockRenderer::new);

    @WrapOperation(
            method = "renderModel",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/fabricmc/fabric/api/renderer/v1/model/FabricBakedModel;emitBlockQuads(Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Ljava/util/function/Supplier;Lnet/fabricmc/fabric/api/renderer/v1/render/RenderContext;)V"
            )
    )
    private void emitTerrainBEs(
            final FabricBakedModel instance,
            final BlockAndTintGetter level,
            final BlockState state,
            final BlockPos pos,
            final Supplier<RandomSource> randomSupplier,
            final RenderContext context,
            final Operation<Void> original
    ) {
        if (!skipVanillaModelForTerrainBE(state)) {
            original.call(instance, level, state, pos, randomSupplier, context);
        }

        final LevelSlice slice = ((AbstractBlockRenderContextAccessor) (Object) this).getSlice();
        if (slice == null) {
            return;
        }

        final MutableQuadViewImpl emitter = (MutableQuadViewImpl) ((AbstractBlockRenderContext) (Object) this).getEmitter();
        TERRAIN_RENDERER.get().emitTerrainBlockEntityGeometry(emitter, slice, pos, state);
    }

    @Unique private static boolean skipVanillaModelForTerrainBE(final BlockState state) {
        if (!ConfigCache.masterOptimize || !state.hasBlockEntity()) {
            return false;
        }

        final Block block = state.getBlock();

        if (block instanceof ChestBlock) {
            if (!ConfigCache.optimizeChests) {
                return false;
            }

            final BlockEntityType<?> type = state.is(Blocks.TRAPPED_CHEST)
                    ? BlockEntityType.TRAPPED_CHEST
                    : BlockEntityType.CHEST;
            return !AltRenderers.hasRendererOverride(type);
        }

        if (block instanceof EnderChestBlock) {
            return ConfigCache.optimizeChests && !AltRenderers.hasRendererOverride(BlockEntityType.ENDER_CHEST);
        }

        if (block instanceof BedBlock) {
            return ConfigCache.optimizeBeds && !AltRenderers.hasRendererOverride(BlockEntityType.BED);
        }

        if (block instanceof ShulkerBoxBlock) {
            return ConfigCache.optimizeShulker && !AltRenderers.hasRendererOverride(BlockEntityType.SHULKER_BOX);
        }

        if (block instanceof BannerBlock || block instanceof WallBannerBlock) {
            return ConfigCache.optimizeBanners && !AltRenderers.hasRendererOverride(BlockEntityType.BANNER);
        }

        if (block instanceof DecoratedPotBlock) {
            return ConfigCache.optimizeDecoratedPots && !AltRenderers.hasRendererOverride(BlockEntityType.DECORATED_POT);
        }

        if (block instanceof StandingSignBlock
                || block instanceof WallSignBlock
                || block instanceof CeilingHangingSignBlock
                || block instanceof WallHangingSignBlock) {
            return ConfigCache.optimizeSigns
                    && !AltRenderers.hasRendererOverride(BlockEntityType.SIGN)
                    && !AltRenderers.hasRendererOverride(BlockEntityType.HANGING_SIGN);
        }

        if (block instanceof BellBlock) {
            return false;
        }

        return false;
    }

    @WrapOperation(method = "bufferQuad",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/caffeinemc/mods/sodium/client/render/chunk/translucent_sorting/TranslucentGeometryCollector;appendQuad([Lnet/caffeinemc/mods/sodium/client/render/chunk/vertex/format/ChunkVertexEncoder$Vertex;Lnet/caffeinemc/mods/sodium/client/model/quad/properties/ModelQuadFacing;I)Z"
            )
    )
    private boolean preserveNoSplitTag(
            final TranslucentGeometryCollector collector,
            final ChunkVertexEncoder.Vertex[] vertices,
            final ModelQuadFacing facing,
            final int packedNormal,
            final Operation<Boolean> original,
            @Local(argsOnly = true) final MutableQuadViewImpl quad
    ) {
        final TranslucentGeometryCollectorExt collectorExt = (TranslucentGeometryCollectorExt) collector;

        try {
            if (quad.tag() == BBEBlockRenderer.NO_QUAD_SPLITTING_TAG) {
                collectorExt.setIncomingQuadSplitMode(QuadSplittingMode.NONE);
            }
            return original.call(collector, vertices, facing, packedNormal);
        } finally {
            collectorExt.deferSplittingMode();
        }
    }
}