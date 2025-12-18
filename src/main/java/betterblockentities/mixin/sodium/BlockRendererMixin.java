package betterblockentities.mixin.sodium;

/* local */
import betterblockentities.BetterBlockEntities;
import betterblockentities.gui.ConfigManager;
import betterblockentities.util.*;

/* minecraft */
import net.caffeinemc.mods.sodium.client.render.helper.ModelHelper;
import net.caffeinemc.mods.sodium.client.render.model.AmbientOcclusionMode;
import net.caffeinemc.mods.sodium.client.services.PlatformBlockAccess;
import net.caffeinemc.mods.sodium.client.services.PlatformModelAccess;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.TriState;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/* sodium */
import net.caffeinemc.mods.sodium.client.model.color.ColorProvider;
import net.caffeinemc.mods.sodium.client.model.color.ColorProviderRegistry;
import net.caffeinemc.mods.sodium.client.model.light.LightMode;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.ChunkBuildBuffers;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderer;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.TerrainRenderPass;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.material.DefaultMaterials;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.material.Material;
import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.TranslucentGeometryCollector;
import net.caffeinemc.mods.sodium.client.render.chunk.vertex.format.ChunkVertexEncoder;
import net.caffeinemc.mods.sodium.client.render.model.AbstractBlockRenderContext;
import net.caffeinemc.mods.sodium.client.render.model.MutableQuadViewImpl;
import net.caffeinemc.mods.sodium.client.render.model.SodiumShadeMode;
import net.caffeinemc.mods.sodium.client.services.PlatformModelEmitter;

/* mixin */
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/* java/misc */
import org.joml.Vector3f;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Pseudo
@Mixin(BlockRenderer.class)
public abstract class BlockRendererMixin extends AbstractBlockRenderContext {
    @Shadow @Final private Vector3f posOffset;
    @Shadow @Nullable private ColorProvider<BlockState> colorProvider;
    @Shadow @Final private ColorProviderRegistry colorProviderRegistry;
    @Shadow protected abstract void tintQuad(MutableQuadViewImpl quad);
    @Shadow protected abstract void bufferQuad(MutableQuadViewImpl quad, float[] brightnesses, Material material);

    /** Override this as we need full control over what get added to this mesh */
    @Overwrite
    public void renderModel(BlockStateModel model, BlockState state, BlockPos pos, BlockPos origin) {
        try {
            Block block = state.getBlock();

            if (BlockEntityManager.isSupportedBlock(block) && !ConfigManager.CONFIG.master_optimize) {
                return;
            }

            /* setup context */
            init(state, pos, origin);

            /* grab emitter */
            final MutableQuadViewImpl emitter = this.getForEmitting();

            /* get an instance to our accessor and create a new helper */
            BlockEntityExt blockEntity = getBlockEntityInstance(pos);
            final BlockRenderHelper helper = new BlockRenderHelper(((BlockRenderer)(Object)this), (BlockEntity)blockEntity);

            /* SINGS  */
            if (block instanceof SignBlock || block instanceof CeilingHangingSignBlock) {
                if (!ConfigManager.CONFIG.optimize_signs) return;

                List<BlockModelPart> parts = model.collectParts(this.random);
                BlockRenderHelper.emitModelPart(parts, emitter, state, this::isFaceCulled, helper::emitSignQuads);
            }
            else if (block instanceof WallHangingSignBlock || block instanceof WallSignBlock) {
                if (!ConfigManager.CONFIG.optimize_signs) return;

                PlatformModelEmitter.getInstance().emitModel(model, this::isFaceCulled, emitter, this.random, this.level, pos, state, this::bufferDefaultModel);
            }

            /* SHULKERS, and CHESTS */
            else if (block instanceof ChestBlock || block instanceof EnderChestBlock || block instanceof ShulkerBoxBlock) {
                boolean isShulker = block instanceof ShulkerBoxBlock;

                if ((isShulker && !ConfigManager.CONFIG.optimize_shulkers) || (!isShulker && !ConfigManager.CONFIG.optimize_chests))
                    return;

                List<BlockModelPart> parts = model.collectParts(this.random);

                /* splice BlockModelParts from MultipartBlockStateModel */
                int quadThreshold = isShulker ? 10 : 6;
                Map<Boolean, List<BlockModelPart>> partitioned = parts.stream()
                        .collect(Collectors.partitioningBy(p -> p.getQuads(null).size() > quadThreshold));

                List<BlockModelPart> lidParts   = partitioned.get(true);
                List<BlockModelPart> trunkParts = partitioned.get(false);

                List<BlockModelPart> merged =  new ArrayList<>();

                BlockEntityExt ext = getBlockEntityInstance(pos);
                boolean shouldRender = shouldRender(ext);

                if (ConfigManager.CONFIG.updateType == 1)
                    merged.addAll(trunkParts);
                else {
                    if (shouldRender)
                        merged.addAll(trunkParts);
                }

                /* merge BlockModelParts after splicing */
                if (shouldRender) merged.addAll(lidParts);

                BlockRenderHelper.emitModelPart(merged, emitter, state, this::isFaceCulled, isShulker ? this::bufferDefaultModel : helper::emitQuadsGE);
            }

            /* BELLS */
            else if (block instanceof BellBlock) {
                List<BlockModelPart> parts = model.collectParts(this.random);

                /*
                    splice BlockModelParts from MultipartBlockStateModel
                    fix this later with proper identifiers (quad size)
                */
                int quadThreshold = 12;
                Map<Boolean, List<BlockModelPart>> partitioned = parts.stream()
                        .collect(Collectors.partitioningBy(p -> p.particleIcon().contents().name().getPath().contains("bottom")));

                List<BlockModelPart> barParts = partitioned.get(true);
                List<BlockModelPart> bellBodyParts = partitioned.get(false);

                List<BlockModelPart> merged =  new ArrayList<>();

                BlockEntityExt ext = getBlockEntityInstance(pos);
                boolean shouldRender = shouldRender(ext);

                merged.addAll(barParts);

                /* merge BlockModelParts after splicing */
                if (shouldRender && ConfigManager.CONFIG.optimize_bells && ConfigManager.CONFIG.master_optimize)
                    merged.addAll(bellBodyParts);

                BlockRenderHelper.emitModelPart(merged, emitter, state, this::isFaceCulled, this::bufferDefaultModel);
            }

            /* DECORATED POT  */
            else if (block instanceof DecoratedPotBlock) {
                if (!ConfigManager.CONFIG.optimize_decoratedpots) return;

                BlockEntityExt ext = getBlockEntityInstance(pos);
                boolean shouldRender = shouldRender(ext);

                if (!shouldRender) return;

                List<BlockModelPart> parts = model.collectParts(this.random);
                BlockRenderHelper.emitModelPart(parts, emitter, state, this::isFaceCulled, helper::emitDecoratedPotQuads);
            }

            /* BED */
            else if (block instanceof BedBlock) {
                if (!ConfigManager.CONFIG.optimize_beds) return;

                PlatformModelEmitter.getInstance().emitModel(model, this::isFaceCulled, emitter, this.random, this.level, pos, state, helper::emitQuadsGE);
            }

            /* BANNER */
            else if (block instanceof BannerBlock || block instanceof WallBannerBlock) {
                if (!ConfigManager.CONFIG.optimize_banners) return;

                List<BlockModelPart> parts = model.collectParts(this.random);
                BlockRenderHelper.emitModelPart(parts, emitter, state, this::isFaceCulled, helper::emitBannerQuads);
            }

            /* ALL OTHER BLOCKS ARE HANDLED HERE */
            else {
                PlatformModelEmitter.getInstance().emitModel(model, this::isFaceCulled, emitter, this.random, this.level, pos, state, this::bufferDefaultModel);
            }
            destory();
        }
        catch (Exception e) {
            BetterBlockEntities.getLogger().error("Error: General fault in BlockRenderer at {}", pos, e);
        }
    }

    @Unique
    void init(BlockState state, BlockPos pos, BlockPos origin) {
        this.state = state;
        this.pos = pos;
        this.prepareAoInfo(true);
        this.posOffset.set((float)origin.getX(), (float)origin.getY(), (float)origin.getZ());
        if (state.hasOffsetFunction()) {
            Vec3 modelOffset = state.getOffset(pos);
            this.posOffset.add((float)modelOffset.x, (float)modelOffset.y, (float)modelOffset.z);
        }

        this.colorProvider = this.colorProviderRegistry.getColorProvider(state.getBlock());
        this.prepareCulling(true);
        this.defaultRenderType = ItemBlockRenderTypes.getChunkRenderType(state);
        this.allowDowngrade = true;
        this.random.setSeed(state.getSeed(pos));
    }

    @Unique
    void destory() {
        this.defaultRenderType = null;
    }

    @Unique
    private boolean shouldRender(BlockEntityExt ext) {
        return ext == null || !ext.getRemoveChunkVariant();
    }

    /* safely retrieve block entity and an instance to our accessor  */
    @Unique
    private BlockEntityExt getBlockEntityInstance(BlockPos pos) {
        try {
            ClientLevel world = Minecraft.getInstance().level;
            BlockEntity blockEntity = world.getBlockEntity(pos);
            return (blockEntity instanceof BlockEntityExt bex) ? bex : null;
        } catch (Exception e) {
            BetterBlockEntities.getLogger().error("Error: Getting Block Entity and accessor at {}", pos, e);
            return null;
        }
    }

    @Override
    protected void processQuad(MutableQuadViewImpl quad) {
        TriState aoMode = quad.ambientOcclusion();
        SodiumShadeMode shadeMode = quad.getShadeMode();
        LightMode lightMode;
        if (aoMode == TriState.DEFAULT) {
            lightMode = this.defaultLightMode;
        } else {
            lightMode = this.useAmbientOcclusion && aoMode != TriState.FALSE ? LightMode.SMOOTH : LightMode.FLAT;
        }

        boolean emissive = quad.emissive();
        ChunkSectionLayer blendMode = quad.getRenderType();
        Material material = DefaultMaterials.forChunkLayer(blendMode == null ? this.defaultRenderType : blendMode);
        this.tintQuad(quad);
        this.shadeQuad(quad, lightMode, emissive, shadeMode);
        this.bufferQuad(quad, this.quadLightData.br, material);
    }
}