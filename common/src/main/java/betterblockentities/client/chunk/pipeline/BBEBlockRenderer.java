package betterblockentities.client.chunk.pipeline;

/* local */
import betterblockentities.client.chunk.section.SectionUpdateDispatcher;
import betterblockentities.client.model.geometry.ModelResourceUtil;
import betterblockentities.client.gui.config.ConfigCache;
import betterblockentities.client.gui.option.EnumTypes;
import betterblockentities.client.model.geometry.GeometryRegistry;
import betterblockentities.client.model.texture.SpriteSelector;
import betterblockentities.client.model.geometry.MultiPartBlockModel;
import betterblockentities.client.render.immediate.blockentity.extentions.BlockEntityExt;
import betterblockentities.client.render.immediate.blockentity.misc.RenderingMode;
import betterblockentities.client.tasks.TaskScheduler;
import betterblockentities.client.tasks.ResourceTasks;
import betterblockentities.platform.GlobalScope;
import betterblockentities.render.AltRenderers;

/* minecraft */
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/* sodium */
import net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderer;
import net.caffeinemc.mods.sodium.client.render.model.MutableQuadViewImpl;
import net.caffeinemc.mods.sodium.client.services.PlatformModelEmitter;
import net.caffeinemc.mods.sodium.client.world.LevelSlice;
import net.caffeinemc.mods.sodium.client.render.model.SodiumShadeMode;

/* java/misc */
import java.util.*;
import java.util.function.Predicate;

/**
 * A wrapper/redirect for {@link net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderer#renderModel} ->
 * {@link net.caffeinemc.mods.sodium.client.services.DefaultModelEmitter#emitModel} which hands over mesh assembly to us
 */

public class BBEBlockRenderer  {
    /* allocate part lists once per instance creation and push;clear accordingly, reduces memory churn A LOT */
    private final ArrayList<BlockStateModelPart> PRIMARY_MODEL_PARTS = new ArrayList<>(64);
    private final ArrayList<BlockStateModelPart> SECONDARY_MODEL_PARTS = new ArrayList<>(64);

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
        if (!ext.bbe$isSupportedBlockEntity()) {
            return;
        }

        final Block block = state.getBlock();

        if (ConfigCache.shadeMode == EnumTypes.ShadeMode.VANILLA.ordinal()) {
            this.emitter.setFlag(BBEEmitter.IMMEDIATE_SHADING);
        }

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

        this.emitter.clear();
    }

    private void emitChest(Predicate<Direction> isFaceCulled, RandomSource random, BlockState state, BBEEmitter emitter, BlockEntity blockEntity) {
        if (!shouldRender((BlockEntityExt)blockEntity)) {
            return;
        }

        final ModelLayerLocation layer = ModelResourceUtil.getChestLayer(state);
        final Map<String, BlockStateModel> pairs = tryGetPairs(layer);

        if (pairs.isEmpty()) {
            return;
        }

        ModelResourceUtil.collectMultiModelParts(PRIMARY_MODEL_PARTS, pairs.values(), random);

        TextureAtlasSprite sprite = SpriteSelector.getChestSprite(state, blockEntity, ConfigCache.christmasChests);

        Direction facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);

        emitter.setShadeMode(SodiumShadeMode.VANILLA);
        emitter.setSprite(sprite);
        emitter.setTransformation(ChestRenderer.modelTransformation(facing));
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

        TextureAtlasSprite sprite = SpriteSelector.getShulkerBoxSprite((ShulkerBoxBlock)state.getBlock());

        Direction facing = state.getValue(BlockStateProperties.FACING);

        emitter.setShadeMode(SodiumShadeMode.VANILLA);
        emitter.setSprite(sprite);
        emitter.setRenderType(ChunkSectionLayer.CUTOUT);
        emitter.setTransformation(ShulkerBoxRenderer.modelTransform(facing));
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

        emitter.setShadeMode(SodiumShadeMode.VANILLA);
        emitter.setSprite(BellRenderer.BELL_TEXTURE);
        emitter.setRenderType(ChunkSectionLayer.SOLID);
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

        emitter.setShadeMode(SodiumShadeMode.VANILLA);
        emitter.setRenderType(ChunkSectionLayer.SOLID);
        emitter.setTransformation(DecoratedPotRenderer.modelTransformation(facing));

        /* emit the base (top and bottom) */
        emitter.setSprite(Sheets.DECORATED_POT_BASE);
        emitter.emit(PRIMARY_MODEL_PARTS, isFaceCulled, emitter::buffer);

        DecoratedPotBlockEntity decoratedPotBlockEntity = (DecoratedPotBlockEntity)blockEntity;

        PotDecorations decorations = decoratedPotBlockEntity.getDecorations();
        for (Map.Entry<String, BlockStateModel> e : sidePairs.entrySet()) {
            String modelName = e.getKey();
            BlockStateModel model = e.getValue();

            ModelResourceUtil.collectSingleModelParts(SECONDARY_MODEL_PARTS, model, random);

            TextureAtlasSprite sideMaterial = switch (modelName) {
                case "back"  -> SpriteSelector.getDecoratedPotSideSprite(decorations.back());
                case "front" -> SpriteSelector.getDecoratedPotSideSprite(decorations.front());
                case "left"  -> SpriteSelector.getDecoratedPotSideSprite(decorations.left());
                case "right" -> SpriteSelector.getDecoratedPotSideSprite(decorations.right());
                default      -> SpriteSelector.getDecoratedPotSideSprite(Optional.empty());
            };

            /* emit sides (patterns) */
            emitter.setSprite(sideMaterial);
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
            emitter.setTransformation(BannerRenderer.TRANSFORMATIONS.wallTransformation(facing));
        }
        else {
            int rotationSegment = state.getValue(BlockStateProperties.ROTATION_16);
            emitter.setTransformation(BannerRenderer.TRANSFORMATIONS.freeTransformations(rotationSegment));
        }

        /* emit pole */
        emitter.setShadeMode(SodiumShadeMode.VANILLA);
        emitter.setSprite(Sheets.BANNER_BASE);
        emitter.setRenderType(ChunkSectionLayer.SOLID);
        emitter.emit(PRIMARY_MODEL_PARTS, isFaceCulled, emitter::buffer);

        int fancy = EnumTypes.BannerGraphicsType.FANCY.ordinal();
        ChunkSectionLayer rt = (ConfigCache.bannerGraphics == fancy)
                ? ChunkSectionLayer.TRANSLUCENT : ChunkSectionLayer.CUTOUT;

        /* emit base canvas */
        emitter.setRenderType(rt);
        emitter.setColor(bannerBlockEntity.getBaseColor().getTextureDiffuseColor());
        emitter.setFlag(BBEEmitter.NO_QUAD_SPLITTING);

        emitter.emit(SECONDARY_MODEL_PARTS, isFaceCulled, emitter::buffer);

        /* emit banner layers */
        for (BannerPatternLayers.Layer layer : bannerBlockEntity.getPatterns().layers()) {
            TextureAtlasSprite sprite = SpriteSelector.getBannerPatternSprite(layer.pattern());
            DyeColor layerColor = layer.color();

            emitter.setSprite(sprite);
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

        TextureAtlasSprite sprite = SpriteSelector.getCopperGolemStatueSprite(cgsBlock);

        Direction facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);

        emitter.setShadeMode(SodiumShadeMode.VANILLA);
        emitter.setSprite(sprite);
        emitter.setRenderType(ChunkSectionLayer.SOLID);
        emitter.setTransformation(CopperGolemStatueBlockRenderer.modelTransformation(facing));
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
            GlobalScope.LOGGER.error("Failed to fetch block entity at {}. " + "LevelSlice#getBlockEntity threw an exception!", pos, e);
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
        return ext.bbe$getRenderingMode() == RenderingMode.TERRAIN;
    }

    private void clearParts() {
        PRIMARY_MODEL_PARTS.clear();
        SECONDARY_MODEL_PARTS.clear();
    }
}
