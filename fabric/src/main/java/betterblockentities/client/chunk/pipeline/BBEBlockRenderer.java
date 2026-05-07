package betterblockentities.client.chunk.pipeline;

/* local */
import betterblockentities.client.BBE;
import betterblockentities.client.chunk.section.SectionUpdateDispatcher;
import betterblockentities.client.chunk.util.ModelResourceUtil;
import betterblockentities.client.gui.config.ConfigCache;
import betterblockentities.client.gui.option.EnumTypes;
import betterblockentities.client.model.MaterialSelector;
import betterblockentities.client.model.MultiPartBlockModel;
import betterblockentities.client.model.geometry.GeometryRegistry;
import betterblockentities.client.render.immediate.blockentity.extentions.BlockEntityExt;
import betterblockentities.client.render.immediate.blockentity.misc.RenderingMode;
import betterblockentities.client.render.immediate.blockentity.renderers.*;
import betterblockentities.client.tasks.ResourceTasks;
import betterblockentities.client.tasks.TaskScheduler;
import betterblockentities.render.AltRenderers;

/* minecraft */
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.blockentity.state.ChestRenderState;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.block.state.properties.WoodType;

/* sodium */
import net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderer;
import net.caffeinemc.mods.sodium.client.render.model.MutableQuadViewImpl;
import net.caffeinemc.mods.sodium.client.services.PlatformModelEmitter;
import net.caffeinemc.mods.sodium.client.world.LevelSlice;

/* java/misc */
import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

public class BBEBlockRenderer  {
    /* allocate part lists once per instance creation and push;clear accordingly, reduces memory churn A LOT */
    private final ArrayList<BlockModelPart> PRIMARY_MODEL_PARTS = new ArrayList<>(64);
    private final ArrayList<BlockModelPart> SECONDARY_MODEL_PARTS = new ArrayList<>(64);

    private final BBEEmitter emitter;

    public BBEBlockRenderer(BlockRenderer sodiumBlockRenderer) {
        this.emitter = new BBEEmitter(sodiumBlockRenderer);
    }

    public void emitBlockModel(PlatformModelEmitter sodiumPlatformEmitter, BlockStateModel model, Predicate<Direction> isFaceCulled, MutableQuadViewImpl sodiumEmitter, RandomSource random, BlockAndTintGetter level, LevelSlice slice, BlockPos pos, BlockState state, PlatformModelEmitter.Bufferer bufferer) {
        /* default path for blocks with block models added through resource packs */
        sodiumPlatformEmitter.emitModel(model, isFaceCulled, sodiumEmitter, random, level, pos, state, bufferer);

        if (!ConfigCache.masterOptimize || !state.hasBlockEntity()) {
            return;
        }

        final BlockEntity blockEntity = tryGetBlockEntity(pos, slice);
        if (blockEntity == null) {
            return;
        }

        /* skip if the declared BlockEntityType is not supported (could be a modded block entity) */
        final BlockEntityExt ext = (BlockEntityExt)blockEntity;
        if (!ext.supportedBlockEntity()) {
            return;
        }

        final Block block = state.getBlock();

        if (block instanceof ChestBlock || block instanceof EnderChestBlock) {
            if (ConfigCache.optimizeChests && !AltRenderers.hasRendererOverride(blockEntity.getType())) {
                emitChest(isFaceCulled, random, state, this.emitter, blockEntity);
            }
        }

        else if (block instanceof ShulkerBoxBlock) {
            if (ConfigCache.optimizeShulker && !AltRenderers.hasRendererOverride(blockEntity.getType())) {
                emitShulker(isFaceCulled, random, state, this.emitter, blockEntity);
            }
        }

        else if (block instanceof CeilingHangingSignBlock || block instanceof WallHangingSignBlock) {
            if (ConfigCache.optimizeSigns && !AltRenderers.hasRendererOverride(blockEntity.getType())) {
                emitHangingSign(isFaceCulled, random, state, this.emitter);
            }
        }

        else if (block instanceof StandingSignBlock || block instanceof WallSignBlock) {
            if (ConfigCache.optimizeSigns && !AltRenderers.hasRendererOverride(blockEntity.getType())) {
                emitSign(isFaceCulled, random, state, this.emitter);
            }
        }

        else if (block instanceof BellBlock) {
            if (ConfigCache.optimizeBells && !AltRenderers.hasRendererOverride(blockEntity.getType())) {
                emitBell(isFaceCulled, random, this.emitter, blockEntity);
            }
        }

        else if (block instanceof DecoratedPotBlock) {
            if (ConfigCache.optimizeDecoratedPots && !AltRenderers.hasRendererOverride(blockEntity.getType())) {
                emitDecoratedPot(isFaceCulled, state, random, this.emitter, blockEntity);
            }
        }

        else if (block instanceof BedBlock) {
            if (ConfigCache.optimizeBeds && !AltRenderers.hasRendererOverride(blockEntity.getType())) {
                emitBed(isFaceCulled, random, state, this.emitter);
            }
        }

        else if (block instanceof WallBannerBlock || block instanceof BannerBlock) {
            if (ConfigCache.optimizeBanners && !AltRenderers.hasRendererOverride(blockEntity.getType())) {
                emitBanner(isFaceCulled, random, state, this.emitter, blockEntity);
            }
        }

        else if (block instanceof CopperGolemStatueBlock) {
            if (ConfigCache.optimizeCopperGolemStatue && !AltRenderers.hasRendererOverride(blockEntity.getType())) {
                emitCopperGolemStatue(isFaceCulled, random, state, this.emitter);
            }
        }
    }

    private void emitChest(Predicate<Direction> isFaceCulled, RandomSource random, BlockState state, BBEEmitter emitter, BlockEntity blockEntity) {
        final ModelLayerLocation layer = ModelResourceUtil.getChestLayer(state);
        final Map<String, BlockStateModel> pairs = tryGetPairs(layer);

        if (pairs.isEmpty()) {
            return;
        }

        ModelResourceUtil.collectSplitModelParts(blockEntity, PRIMARY_MODEL_PARTS, pairs, random);

        final boolean christmas = ConfigCache.christmasChests;
        ChestRenderState.ChestMaterialType chestMat = MaterialSelector.getChestMaterial(blockEntity, christmas);
        final ChestType type = state.hasProperty(ChestBlock.TYPE) ? state.getValue(ChestBlock.TYPE) : ChestType.SINGLE;
        final Material material = Sheets.chooseMaterial(chestMat, type);

        Direction facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);

        emitter.setMaterial(material);
        emitter.setTransformation(BBEChestRenderer.modelTransformation(facing));
        emitter.setRenderType(ChunkSectionLayer.SOLID);
        emitter.emit(PRIMARY_MODEL_PARTS, isFaceCulled, emitter::buffer);

        clearParts();
        emitter.clear();
    }

    private void emitShulker(Predicate<Direction> isFaceCulled, RandomSource random, BlockState state, BBEEmitter emitter, BlockEntity blockEntity) {
        if (!shouldRender((BlockEntityExt)blockEntity)) {
            return;
        }

        final ModelLayerLocation layer = ModelResourceUtil.getShulkerBoxLayer();
        final Map<String, BlockStateModel> pairs = tryGetPairs(layer);

        if (pairs.isEmpty()) {
            return;
        }

        ModelResourceUtil.collectMultiModelParts(PRIMARY_MODEL_PARTS, pairs.values(), random);

        DyeColor color = ((ShulkerBoxBlock) state.getBlock()).getColor();
        Material shulkerMaterial = (color == null) ? Sheets.DEFAULT_SHULKER_TEXTURE_LOCATION : Sheets.getShulkerBoxMaterial(color);

        Direction facing = state.getValue(BlockStateProperties.FACING);

        emitter.setMaterial(shulkerMaterial);
        emitter.setRenderType(ChunkSectionLayer.CUTOUT);
        emitter.setTransformation(BBEShulkerBoxRenderer.modelTransform(facing));
        emitter.emit(PRIMARY_MODEL_PARTS, isFaceCulled, emitter::buffer);

        clearParts();
        emitter.clear();
    }

    private void emitSign(Predicate<Direction> isFaceCulled, RandomSource random, BlockState state, BBEEmitter emitter) {
        final ModelLayerLocation layerLocation = ModelResourceUtil.getSignLayer(state);
        final Map<String, BlockStateModel> pairs = tryGetPairs(layerLocation);

        if (pairs.isEmpty()) {
            return;
        }

        ModelResourceUtil.collectMultiModelParts(PRIMARY_MODEL_PARTS, pairs.values(), random);

        WoodType woodType = ((SignBlock) state.getBlock()).type();
        Material signMaterial = Sheets.getSignMaterial(woodType);

        final boolean isWallSign = !state.hasProperty(BlockStateProperties.ROTATION_16);

        emitter.setMaterial(signMaterial);
        emitter.setRenderType(ChunkSectionLayer.SOLID);

        if (isWallSign) {
            Direction facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
            emitter.setTransformation(
                    BBEStandingSignRenderer.TRANSFORMATIONS.wallTransformation(facing).body()
            );
        }
        else {
            int rotationSegment = state.getValue(BlockStateProperties.ROTATION_16);
            emitter.setTransformation(
                    BBEStandingSignRenderer.TRANSFORMATIONS.freeTransformations(rotationSegment).body()
            );
        }

        emitter.emit(PRIMARY_MODEL_PARTS, isFaceCulled, emitter::buffer);

        clearParts();
        emitter.clear();
    }

    private void emitHangingSign(Predicate<Direction> isFaceCulled, RandomSource random, BlockState state, BBEEmitter emitter) {
        final boolean isWall = !state.hasProperty(CeilingHangingSignBlock.ATTACHED);
        final boolean attached = !isWall && state.getValue(CeilingHangingSignBlock.ATTACHED);

        final ModelLayerLocation layerLocation = ModelResourceUtil.getHangingSignLayer(state);
        final Map<String, BlockStateModel> pairs = tryGetPairs(layerLocation);

        if (pairs.isEmpty()) {
            return;
        }

        ModelResourceUtil.collectMultiModelParts(PRIMARY_MODEL_PARTS, pairs.values(), random);

        WoodType woodType = ((SignBlock) state.getBlock()).type();
        Material signMaterial = Sheets.getHangingSignMaterial(woodType);

        emitter.setMaterial(signMaterial);
        emitter.setRenderType(ChunkSectionLayer.CUTOUT);

        /* invert chains to fix backface culling issue, this is shit but hey >) */
        BlockStateModel chains = pairs.get(attached ? "vChains" : "normalChains");
        if (chains != null) {
            ModelResourceUtil.collectSingleModelParts(SECONDARY_MODEL_PARTS, chains, random);

            if (!isWall) {
                int rotationSegment = state.getValue(BlockStateProperties.ROTATION_16);
                int oppositeSegment = (rotationSegment + 8) & 15;
                emitter.setTransformation(
                        BBEHangingSignRenderer.TRANSFORMATIONS.freeTransformations(oppositeSegment).body()
                );
            }
            else {
                Direction facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING).getOpposite();
                emitter.setTransformation(
                        BBEHangingSignRenderer.TRANSFORMATIONS.wallTransformation(facing).body()
                );
            }
            emitter.emit(SECONDARY_MODEL_PARTS, isFaceCulled, emitter::buffer);
        }

        if (!isWall) {
            int rotationSegment = state.getValue(BlockStateProperties.ROTATION_16);
            emitter.setTransformation(BBEHangingSignRenderer.TRANSFORMATIONS.freeTransformations(rotationSegment).body());
        }
        else {
            Direction facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
            emitter.setTransformation(BBEHangingSignRenderer.TRANSFORMATIONS.wallTransformation(facing).body());
        }

        emitter.emit(PRIMARY_MODEL_PARTS, isFaceCulled, emitter::buffer);

        clearParts();
        emitter.clear();
    }

    private void emitBell(Predicate<Direction> isFaceCulled, RandomSource random, BBEEmitter emitter, BlockEntity blockEntity) {
        if (!shouldRender((BlockEntityExt)blockEntity)) {
            return;
        }

        final ModelLayerLocation layer = ModelResourceUtil.getBellLayer();
        final Map<String, BlockStateModel> pairs = tryGetPairs(layer);

        if (pairs.isEmpty()) {
            return;
        }

        pairs.values().forEach((model) -> {
            ModelResourceUtil.collectSingleModelParts(PRIMARY_MODEL_PARTS, model, random);
        });

        Material bellBodyMaterial = BBEBellRenderer.BELL_TEXTURE;

        emitter.setMaterial(bellBodyMaterial);
        emitter.setRenderType(ChunkSectionLayer.SOLID);
        emitter.emit(PRIMARY_MODEL_PARTS, isFaceCulled, emitter::buffer);

        clearParts();
        emitter.clear();
    }

    private void emitBed(Predicate<Direction> isFaceCulled, RandomSource random, BlockState state, BBEEmitter emitter) {
        final ModelLayerLocation layer = ModelResourceUtil.getBedLayer(state);
        final Map<String, BlockStateModel> pairs = tryGetPairs(layer);

        if (pairs.isEmpty()) {
            return;
        }

        ModelResourceUtil.collectMultiModelParts(PRIMARY_MODEL_PARTS, pairs.values(), random);

        DyeColor color = ((BedBlock) state.getBlock()).getColor();
        Material bedMaterial = Sheets.getBedMaterial(color);

        Direction facing = state.getValue(BedBlock.FACING);

        emitter.setMaterial(bedMaterial);
        emitter.setRenderType(ChunkSectionLayer.SOLID);
        emitter.setTransformation(BBEBedRenderer.modelTransform(facing));
        emitter.emit(PRIMARY_MODEL_PARTS, isFaceCulled, emitter::buffer);

        clearParts();
        emitter.clear();
    }

    private void emitDecoratedPot(Predicate<Direction> isFaceCulled, BlockState state, RandomSource random, BBEEmitter emitter, BlockEntity blockEntity) {
        if (!shouldRender((BlockEntityExt)blockEntity)) {
            return;
        }

        final ModelLayerLocation baseLayer = ModelResourceUtil.getDecoratedPotBaseLayer();
        final ModelLayerLocation sideLayer = ModelResourceUtil.getDecoratedPotSideLayer();

        final Map<String, BlockStateModel> basePairs = tryGetPairs(baseLayer);
        final Map<String, BlockStateModel> sidePairs = tryGetPairs(sideLayer);

        if (basePairs.isEmpty() || sidePairs.isEmpty()) {
            return;
        }

        ModelResourceUtil.collectMultiModelParts(PRIMARY_MODEL_PARTS, basePairs.values(), random);

        Direction facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);

        emitter.setRenderType(ChunkSectionLayer.SOLID);
        emitter.setTransformation(BBEDecoratedPotRenderer.modelTransformation(facing));

        /* emit the base (top and bottom) */
        emitter.setMaterial(Sheets.DECORATED_POT_BASE);
        emitter.emit(PRIMARY_MODEL_PARTS, isFaceCulled, emitter::buffer);

        DecoratedPotBlockEntity decoratedPotBlockEntity = (DecoratedPotBlockEntity)blockEntity;

        PotDecorations decorations = decoratedPotBlockEntity.getDecorations();
        for (Map.Entry<String, BlockStateModel> e : sidePairs.entrySet()) {
            String modelName = e.getKey();
            BlockStateModel model = e.getValue();

            ModelResourceUtil.collectSingleModelParts(SECONDARY_MODEL_PARTS, model, random);

            Material sideMaterial = switch (modelName) {
                case "back"  -> MaterialSelector.getDPSideMaterial(decorations.back());
                case "front" -> MaterialSelector.getDPSideMaterial(decorations.front());
                case "left"  -> MaterialSelector.getDPSideMaterial(decorations.left());
                case "right" -> MaterialSelector.getDPSideMaterial(decorations.right());
                default      -> MaterialSelector.getDPSideMaterial(Optional.empty());
            };

            /* emit sides (patterns) */
            emitter.setMaterial(sideMaterial);
            emitter.emit(SECONDARY_MODEL_PARTS, isFaceCulled, emitter::buffer);

            clearParts();
        }
        emitter.clear();
    }

    /*
     * strict internal draw order is VERY important here and needs to be preserved in the order each layer is emitted
     * because each banner layer is coplanar and therefore exist at the same preceived depth. its know that certain
     * translucent sorting systems can mess this up and cause strange z-fighting like artifacts by rearranging the
     * translucent quads in a strange manner. Sodium has implemented code in their translucent sorting system to account
     * for this, fortunately :). This implementation might still fail with certain shader-packs because of coplanar geometry +
     * the draw state of the translucent render-pass (depth test on, depth write off)
     */
    private void emitBanner(Predicate<Direction> isFaceCulled, RandomSource random, BlockState state, BBEEmitter emitter, BlockEntity blockEntity) {
        final boolean isWallBanner = !state.hasProperty(BlockStateProperties.ROTATION_16);
        final ModelLayerLocation baseLayer = ModelResourceUtil.getBannerBaseLayer(isWallBanner);
        final ModelLayerLocation flagLayer = ModelResourceUtil.getBannerFlagLayer(isWallBanner);

        final Map<String, BlockStateModel> basePairs = tryGetPairs(baseLayer);
        final Map<String, BlockStateModel> canvasPairs = tryGetPairs(flagLayer);

        if (basePairs.isEmpty() || canvasPairs.isEmpty()) {
            return;
        }

        ModelResourceUtil.collectMultiModelParts(PRIMARY_MODEL_PARTS, basePairs.values(), random);
        ModelResourceUtil.collectMultiModelParts(SECONDARY_MODEL_PARTS, canvasPairs.values(), random);

        BannerBlockEntity bannerBlockEntity = (BannerBlockEntity)blockEntity;

        if (isWallBanner) {
            Direction facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
            emitter.setTransformation(BBEBannerRenderer.TRANSFORMATIONS.wallTransformation(facing));
        }
        else {
            int rotationSegment = state.getValue(BlockStateProperties.ROTATION_16);
            emitter.setTransformation(BBEBannerRenderer.TRANSFORMATIONS.freeTransformations(rotationSegment));
        }

        /* emit pole */
        emitter.setMaterial(ModelBakery.BANNER_BASE);
        emitter.setRenderType(ChunkSectionLayer.SOLID);
        emitter.emit(PRIMARY_MODEL_PARTS, isFaceCulled, emitter::buffer);

        final int fancy = EnumTypes.BannerGraphicsType.FANCY.ordinal();
        final ChunkSectionLayer rt = (ConfigCache.bannerGraphics == fancy) ? ChunkSectionLayer.TRANSLUCENT : ChunkSectionLayer.CUTOUT;

        /* emit base canvas */
        emitter.setRenderType(rt);
        emitter.setColor(bannerBlockEntity.getBaseColor().getTextureDiffuseColor());
        emitter.setSplittingMode(BBEEmitter.QuadSplittingMode.NONE);

        emitter.emit(SECONDARY_MODEL_PARTS, isFaceCulled, emitter::buffer);

        /* emit banner layers */
        for (BannerPatternLayers.Layer layer : bannerBlockEntity.getPatterns().layers()) {
            Material layerMaterial = MaterialSelector.getBannerMaterial(layer.pattern());
            DyeColor layerColor = layer.color();

            emitter.setMaterial(layerMaterial);
            emitter.setColor(layerColor.getTextureDiffuseColor());
            emitter.emit(SECONDARY_MODEL_PARTS, isFaceCulled, emitter::buffer);
        }

        clearParts();
        emitter.clear();
    }

    private void emitCopperGolemStatue(Predicate<Direction> isFaceCulled, RandomSource random, BlockState state, BBEEmitter emitter) {
        final ModelLayerLocation layerLocation = ModelResourceUtil.getCGSLayer(state);
        final Map<String, BlockStateModel> pairs = tryGetPairs(layerLocation);

        if (pairs.isEmpty()) {
            return;
        }

        ModelResourceUtil.collectMultiModelParts(PRIMARY_MODEL_PARTS, pairs.values(), random);

        CopperGolemStatueBlock cgsBlock = (CopperGolemStatueBlock)state.getBlock();
        TextureAtlasSprite sprite = ModelResourceUtil.getCGSSprite(cgsBlock);

        Direction facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);

        emitter.setSprite(sprite);
        emitter.setRenderType(ChunkSectionLayer.SOLID);
        emitter.setTransformation(BBECopperGolemStatueBlockRenderer.modelTransformation(facing));
        emitter.emit(PRIMARY_MODEL_PARTS, isFaceCulled, emitter::buffer);

        clearParts();
        emitter.clear();
    }

    /*
     *  some servers can send invalid block data which makes LevelSlice#getBlockEntity
     *  return null. guard against potential exception as well
     */
    private static BlockEntity tryGetBlockEntity(BlockPos pos, LevelSlice slice) {
        try {
            return slice.getBlockEntity(pos);
        } catch (RuntimeException e) {
            BBE.getLogger().error("Failed to fetch block entity at {}. " + "LevelSlice#getBlockEntity threw an exception!", pos, e);
            throw e;
        }
    }

    private static Map<String, BlockStateModel> tryGetPairs(ModelLayerLocation location) {
        try {
            MultiPartBlockModel model = (MultiPartBlockModel)GeometryRegistry.getModel(location);
            return model.getPairs();
        } catch (Exception e) {
            TaskScheduler.schedule(() -> {
                if (ResourceTasks.populateGeometryRegistry() == ResourceTasks.FAILED) {
                    throw new RuntimeException("Failed to repopulate geometry registry after failed location lookup!");
                }
                SectionUpdateDispatcher.queueUpdateAllSections();
            });
            return Map.of();
        }
    }

    public static boolean shouldRender(BlockEntityExt ext) {
        return ext.renderingMode() == RenderingMode.TERRAIN;
    }

    private void clearParts() {
        PRIMARY_MODEL_PARTS.clear();
        SECONDARY_MODEL_PARTS.clear();
    }
}
