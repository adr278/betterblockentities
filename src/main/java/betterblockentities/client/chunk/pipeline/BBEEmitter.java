package betterblockentities.client.chunk.pipeline;

/* local */
import betterblockentities.client.BBE;
import betterblockentities.client.chunk.util.QuadTransform;
import betterblockentities.client.gui.config.ConfigCache;
import betterblockentities.client.gui.option.EnumTypes;
import betterblockentities.client.model.geometry.GeometryRegistry;
import betterblockentities.client.model.MaterialSelector;
import betterblockentities.client.model.MultiPartBlockModel;
import betterblockentities.client.render.immediate.blockentity.BlockEntityExt;
import betterblockentities.client.render.immediate.blockentity.BlockEntityManager;
import betterblockentities.client.tasks.TaskScheduler;
import betterblockentities.client.tasks.Tasks;

/* minecraft */
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.model.*;
import net.minecraft.client.renderer.blockentity.state.ChestRenderState;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.animal.golem.CopperGolemOxidationLevels;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.block.state.properties.WoodType;

/* sodium */
import net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderer;
import net.caffeinemc.mods.sodium.client.render.model.MutableQuadViewImpl;
import net.caffeinemc.mods.sodium.client.services.PlatformModelEmitter;
import net.caffeinemc.mods.sodium.client.world.LevelSlice;

/* java/misc */
import java.util.*;
import java.util.function.Predicate;
import org.jetbrains.annotations.Nullable;

/**
 * A wrapper/redirect for {@link net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderer#renderModel} ->
 * {@link net.caffeinemc.mods.sodium.client.services.DefaultModelEmitter#emitModel} which hands over mesh assembly to us
 */
public class BBEEmitter {
    public static void emit(PlatformModelEmitter instance, BlockStateModel model, Predicate<Direction> isFaceCulled, MutableQuadViewImpl emitter, RandomSource random, BlockAndTintGetter level, LevelSlice slice, BlockPos pos, BlockState state, PlatformModelEmitter.Bufferer bufferer, BlockRenderer blockRenderer) {
        Block block = state.getBlock();

        /* not a valid block (regular terrain or not supported) emit like normal */
        if (!BlockEntityManager.isSupportedBlock(block) || !ConfigCache.masterOptimize) {
            instance.emitModel(model, isFaceCulled, emitter, random, level, pos, state, bufferer);
            return;
        }

        /* invalid block entity, abort */
        BlockEntity blockEntity = tryGetBlockEntity(pos, level, slice);
        if (blockEntity == null) {
            return;
        }

        final BlockRenderHelper helper = new BlockRenderHelper(blockRenderer);

        /* NON EMISSIVE CHESTS */
        if (block instanceof ChestBlock) {
            if (ConfigCache.optimizeChests)
                emitChest(isFaceCulled, emitter, random, pos, state, helper, false, blockEntity);
        }

        /* EMISSIVE CHESTS (Ender) */
        else if (block instanceof EnderChestBlock) {
            if (ConfigCache.optimizeChests)
                emitChest(isFaceCulled, emitter, random, pos, state, helper, true, blockEntity);
        }

        /* SHULKER BOX */
        else if (block instanceof ShulkerBoxBlock) {
            if (ConfigCache.optimizeShulker)
                emitShulker(isFaceCulled, emitter, random, pos, state, helper, blockEntity);
        }

        /* 16 STEP ROTATION SIGNS */
        else if (block instanceof CeilingHangingSignBlock || block instanceof WallHangingSignBlock) {
            if (ConfigCache.optimizeSigns)
                emitHangingSign(isFaceCulled, emitter, random, pos, state, helper);
        }

        /* CARDINAL SIGNS */
        else if (block instanceof WallSignBlock || block instanceof StandingSignBlock) {
            if (ConfigCache.optimizeSigns)
                emitSign(isFaceCulled, emitter, random, pos, state, helper);
        }

        /* BELL */
        else if (block instanceof BellBlock) {
            if (ConfigCache.optimizeBells)
                emitBell(isFaceCulled, emitter, random, pos, state, helper, blockEntity);
        }

        /* DECORATED POT */
        else if (block instanceof DecoratedPotBlock) {
            if (ConfigCache.optimizeDecoratedPots)
                emitDecoratedPot(isFaceCulled, emitter, random, pos, state, helper, blockEntity);
        }

        /* BED */
        else if (block instanceof BedBlock) {
            if (ConfigCache.optimizeBeds)
                emitBed(isFaceCulled, emitter, random, state, helper);
        }

        /* BANNERS */
        else if (block instanceof BannerBlock || block instanceof WallBannerBlock) {
            if (ConfigCache.optimizeBanners)
                emitBanner(isFaceCulled, emitter, random, pos, state, helper, blockEntity);
        }

        else if (block instanceof CopperGolemStatueBlock) {
            if (ConfigCache.optimizeCopperGolemStatue)
                emitCopperGolemStatue(isFaceCulled, emitter, random, pos, state, helper);
        }

        /* emit any accessory parts if there are any */
        if (!model.collectParts(random).isEmpty())
            instance.emitModel(model, isFaceCulled, emitter, random, level, pos, state, bufferer);
    }

    private static void emitChest(Predicate<Direction> isFaceCulled, MutableQuadViewImpl emitter, RandomSource random, BlockPos pos, BlockState state, BlockRenderHelper helper, boolean emissive, BlockEntity blockEntity) {
        ModelLayerLocation layer = ModelLayers.CHEST;

        if (!emissive) {
            layer = switch (state.getValue(ChestBlock.TYPE)) {
                case LEFT  -> ModelLayers.DOUBLE_CHEST_LEFT;
                case RIGHT -> ModelLayers.DOUBLE_CHEST_RIGHT;
                default    -> ModelLayers.CHEST;
            };
        }

        Map<String, BlockStateModel> pairs = tryGetPairs(layer);
        if (pairs.isEmpty()) return;

        int updateType = ConfigCache.updateType;
        boolean drawLid = shouldRender((BlockEntityExt)blockEntity);
        boolean addBase = updateType == EnumTypes.UpdateSchedulerType.FAST.ordinal() ||
                (drawLid && updateType == EnumTypes.UpdateSchedulerType.SMART.ordinal());

        List<BlockModelPart> merged = new ArrayList<>();
        if (addBase) merged.addAll(pairs.get("bottom").collectParts(random));
        if (drawLid) {
            merged.addAll(pairs.get("lid").collectParts(random));
            merged.addAll(pairs.get("lock").collectParts(random));
        }

        boolean christmas = ConfigCache.christmasChests;
        ChestRenderState.ChestMaterialType ChestMaterial = MaterialSelector.getChestMaterial(blockEntity, christmas);
        ChestType type = state.hasProperty(ChestBlock.TYPE) ? state.getValue(ChestBlock.TYPE) : ChestType.SINGLE;
        Material material = Sheets.chooseMaterial(ChestMaterial, type);

        helper.setMaterial(material);
        helper.setRendertype(ChunkSectionLayer.SOLID);
        BlockRenderHelper.emitModelPart(merged, emitter, state, isFaceCulled, helper::emitGE);
    }

    private static void emitShulker(Predicate<Direction> isFaceCulled, MutableQuadViewImpl emitter, RandomSource random, BlockPos pos, BlockState state, BlockRenderHelper helper, BlockEntity blockEntity) {
        Map<String, BlockStateModel> pairs = tryGetPairs(ModelLayers.SHULKER_BOX);
        if (pairs.isEmpty()) return;

        int updateType = ConfigCache.updateType;
        boolean drawLid = shouldRender((BlockEntityExt)blockEntity);
        boolean addBase = updateType == EnumTypes.UpdateSchedulerType.FAST.ordinal() ||
                (drawLid && updateType == EnumTypes.UpdateSchedulerType.SMART.ordinal());

        List<BlockModelPart> merged = new ArrayList<>();
        if (addBase) merged.addAll(pairs.get("base").collectParts(random));
        if (drawLid) merged.addAll(pairs.get("lid").collectParts(random));

        Direction facing = state.hasProperty(ShulkerBoxBlock.FACING) ? state.getValue(ShulkerBoxBlock.FACING) : Direction.UP;
        float[] rotation = switch (facing) {
            case UP    -> new float[]{180, 180};
            case DOWN  -> new float[]{0, 180};
            case NORTH -> new float[]{90, 0};
            case SOUTH -> new float[]{90, 180};
            case WEST  -> new float[]{90, 270};
            case EAST  -> new float[]{90, 90};
        };

        DyeColor color = ((ShulkerBoxBlock)state.getBlock()).getColor();
        Material shulkerMaterial = color == null ? Sheets.DEFAULT_SHULKER_TEXTURE_LOCATION : Sheets.getShulkerBoxMaterial(color);

        helper.setMaterial(shulkerMaterial);
        helper.setRendertype(ChunkSectionLayer.CUTOUT);
        helper.setRotation(rotation);
        BlockRenderHelper.emitModelPart(merged, emitter, state, isFaceCulled, helper::emitGE);
    }

    private static void emitSign(Predicate<Direction> isFaceCulled, MutableQuadViewImpl emitter, RandomSource random, BlockPos pos, BlockState state, BlockRenderHelper helper) {
        boolean isWallSign = !state.hasProperty(BlockStateProperties.ROTATION_16);

        ModelLayerLocation layerLocation = isWallSign ? GeometryRegistry.SupportedVanillaModelLayers.SIGN_WALL : GeometryRegistry.SupportedVanillaModelLayers.SIGN_STANDING;

        Map<String, BlockStateModel> pairs = tryGetPairs(layerLocation);
        if (pairs.isEmpty()) return;

        List<BlockModelPart> merged = pairs.values().stream().flatMap(model -> model.collectParts(random).stream()).toList();

        WoodType woodType = ((SignBlock)state.getBlock()).type();
        Material signMaterial = Sheets.getSignMaterial(woodType);

        helper.setMaterial(signMaterial);
        helper.setRendertype(ChunkSectionLayer.SOLID);
        BlockRenderHelper.emitModelPart(merged, emitter, state, isFaceCulled, helper::emitGE);
    }

    private static void emitHangingSign(Predicate<Direction> isFaceCulled, MutableQuadViewImpl emitter, RandomSource random, BlockPos pos, BlockState state, BlockRenderHelper helper) {
        boolean isWall = !state.hasProperty(CeilingHangingSignBlock.ATTACHED);
        boolean attached = !isWall && state.getValue(CeilingHangingSignBlock.ATTACHED);

        ModelLayerLocation layerLocation1 = isWall ? GeometryRegistry.SupportedVanillaModelLayers.HANGING_SIGN_WALL
                : attached ? GeometryRegistry.SupportedVanillaModelLayers.HANGING_SIGN_CEILING_MIDDLE : GeometryRegistry.SupportedVanillaModelLayers.HANGING_SIGN_CEILING;

        Map<String, BlockStateModel> pairs = tryGetPairs(layerLocation1);
        if (pairs.isEmpty()) return;

        List<BlockModelPart> merged = new java.util.ArrayList<>(
                pairs.values().stream().flatMap(m -> m.collectParts(random).stream()).toList()
        );

        WoodType woodType = ((SignBlock)state.getBlock()).type();
        Material signMaterial = Sheets.getHangingSignMaterial(woodType);

        /* backface culling fix for chains... vanilla chains "parts" are only one quad thick, so we need to double render them but inverted */
        BlockStateModel chains = pairs.get(attached ? "vChains" : "normalChains");
        if (chains != null) {
            List<BlockModelPart> chainParts = chains.collectParts(random);
            float[] rotation = { 0, (BlockRenderHelper.getRotationFromBlockState(state) + 180) % 360 };

            helper.setRotation(rotation);
            helper.setMaterial(signMaterial);
            helper.setRendertype(ChunkSectionLayer.CUTOUT);
            BlockRenderHelper.emitModelPart(chainParts, emitter, state, isFaceCulled, helper::emitGE);
        }
        helper.setRotation(null);
        helper.setMaterial(signMaterial);
        helper.setRendertype(ChunkSectionLayer.CUTOUT);
        BlockRenderHelper.emitModelPart(merged, emitter, state, isFaceCulled, helper::emitGE);
    }

    private static void emitBell(Predicate<Direction> isFaceCulled, MutableQuadViewImpl emitter, RandomSource random, BlockPos pos, BlockState state, BlockRenderHelper helper, BlockEntity blockEntity) {
        if (!shouldRender((BlockEntityExt)blockEntity)) return;

        Map<String, BlockStateModel> pairs = tryGetPairs(ModelLayers.BELL);
        if (pairs.isEmpty()) return;

        List<BlockModelPart> bellBodyParts = pairs.get("bell_body").collectParts(random);

        Material bellBodyMaterial = Sheets.BLOCK_ENTITIES_MAPPER.defaultNamespaceApply("bell/bell_body");

        helper.setMaterial(bellBodyMaterial);
        helper.setRendertype(ChunkSectionLayer.SOLID);
        BlockRenderHelper.emitModelPart(bellBodyParts, emitter, state, isFaceCulled, helper::emitGE);
    }

    private static void emitBed(Predicate<Direction> isFaceCulled, MutableQuadViewImpl emitter, RandomSource random, BlockState state, BlockRenderHelper helper) {
        ModelLayerLocation layer = state.getValue(BedBlock.PART) == BedPart.HEAD ? ModelLayers.BED_HEAD : ModelLayers.BED_FOOT;

        Map<String, BlockStateModel> pairs = tryGetPairs(layer);
        if (pairs.isEmpty()) return;

        List<BlockModelPart> merged = pairs.values().stream().flatMap(model -> model.collectParts(random).stream()).toList();

        DyeColor color = ((BedBlock)state.getBlock()).getColor();
        Material bedMaterial = Sheets.getBedMaterial(color);

        helper.setMaterial(bedMaterial);
        helper.setRendertype(ChunkSectionLayer.SOLID);
        BlockRenderHelper.emitModelPart(merged, emitter, state, isFaceCulled, helper::emitGE);
    }

    private static void emitDecoratedPot(Predicate<Direction> isFaceCulled, MutableQuadViewImpl emitter, RandomSource random, BlockPos pos, BlockState state, BlockRenderHelper helper, BlockEntity blockEntity) {
        if (!shouldRender((BlockEntityExt)blockEntity)) return;

        ModelLayerLocation[] layers = { ModelLayers.DECORATED_POT_BASE, ModelLayers.DECORATED_POT_SIDES };

        Map<String, BlockStateModel> basePairs = tryGetPairs(layers[0]);
        Map<String, BlockStateModel> sidePairs = tryGetPairs(layers[1]);

        if (basePairs.isEmpty() || sidePairs.isEmpty()) return;

        List<BlockModelPart> baseParts = new java.util.ArrayList<>(basePairs.values().stream().flatMap(m -> m.collectParts(random).stream()).toList());

        helper.setMaterial(Sheets.DECORATED_POT_BASE);
        helper.setRendertype(ChunkSectionLayer.SOLID);
        BlockRenderHelper.emitModelPart(baseParts, emitter, state, isFaceCulled, helper::emitGE);

        PotDecorations decorations = ((DecoratedPotBlockEntity)blockEntity).getDecorations();
        sidePairs.forEach((key, model) -> {
            List<BlockModelPart> sideParts = model.collectParts(random);
            Material sideMaterial = switch (key) {
                case "back"  -> MaterialSelector.getDPSideMaterial(decorations.back());
                case "front" -> MaterialSelector.getDPSideMaterial(decorations.front());
                case "left"  -> MaterialSelector.getDPSideMaterial(decorations.left());
                case "right" -> MaterialSelector.getDPSideMaterial(decorations.right());
                default      -> MaterialSelector.getDPSideMaterial(Optional.empty());
            };
            helper.setMaterial(sideMaterial);
            helper.setRendertype(ChunkSectionLayer.SOLID);
            BlockRenderHelper.emitModelPart(sideParts, emitter, state, isFaceCulled, helper::emitGE);
        });
    }

    private static void emitBanner(Predicate<Direction> isFaceCulled, MutableQuadViewImpl emitter, RandomSource random, BlockPos pos, BlockState state, BlockRenderHelper helper, BlockEntity blockEntity) {
        BannerBlockEntity bannerBlockEntity = (BannerBlockEntity)blockEntity;

        boolean isWallBanner = !state.hasProperty(BlockStateProperties.ROTATION_16);
        ModelLayerLocation layerLocation = isWallBanner ? ModelLayers.WALL_BANNER : ModelLayers.STANDING_BANNER;
        ModelLayerLocation layerLocation2 = isWallBanner ? ModelLayers.WALL_BANNER_FLAG : ModelLayers.STANDING_BANNER_FLAG;

        Map<String, BlockStateModel> basePairs = tryGetPairs(layerLocation);
        Map<String, BlockStateModel> canvasPairs = tryGetPairs(layerLocation2);

        if (basePairs.isEmpty() || canvasPairs.isEmpty()) return;

        List<BlockModelPart> baseParts = basePairs.values().stream().flatMap(model -> model.collectParts(random).stream()).toList();
        List<BlockModelPart> canvasParts = canvasPairs.values().stream().flatMap(model -> model.collectParts(random).stream()).toList();

        helper.setMaterial(ModelBakery.BANNER_BASE);
        helper.setRendertype(ChunkSectionLayer.SOLID);
        BlockRenderHelper.emitModelPart(baseParts, emitter, state, isFaceCulled, helper::emitGE);

        helper.setColor(bannerBlockEntity.getBaseColor().getTextureDiffuseColor());
        BlockRenderHelper.emitModelPart(canvasParts, emitter, state, isFaceCulled, helper::emitGE);

        for (BannerPatternLayers.Layer layer : bannerBlockEntity.getPatterns().layers()) {
            Material layerMaterial = Sheets.getBannerMaterial(layer.pattern());
            DyeColor layerColor = layer.color();

            helper.setMaterial(layerMaterial);
            helper.setRendertype(ConfigCache.bannerGraphics == EnumTypes.BannerGraphicsType.FANCY.ordinal() ?
                    ChunkSectionLayer.TRANSLUCENT : ChunkSectionLayer.CUTOUT);
            helper.setColor(layerColor.getTextureDiffuseColor());
            BlockRenderHelper.emitModelPart(canvasParts, emitter, state, isFaceCulled, helper::emitGE);
        }
    }

    private static void emitCopperGolemStatue(Predicate<Direction> isFaceCulled, MutableQuadViewImpl emitter, RandomSource random, BlockPos pos, BlockState state, BlockRenderHelper helper) {
        ModelLayerLocation layerLocation = ModelLayers.COPPER_GOLEM;

        CopperGolemStatueBlock.Pose pose = state.getValue(BlockStateProperties.COPPER_GOLEM_POSE);
        switch (pose) {
            case CopperGolemStatueBlock.Pose.SITTING  -> layerLocation = ModelLayers.COPPER_GOLEM_SITTING;
            case CopperGolemStatueBlock.Pose.RUNNING  -> layerLocation = ModelLayers.COPPER_GOLEM_RUNNING;
            case CopperGolemStatueBlock.Pose.STAR -> layerLocation = ModelLayers.COPPER_GOLEM_STAR;
        }

        Map<String, BlockStateModel> pairs = tryGetPairs(layerLocation);
        if (pairs.isEmpty()) return;

        List<BlockModelPart> merged = pairs.values().stream().flatMap(model -> model.collectParts(random).stream()).toList();

        Direction facing = state.getValue(CopperGolemStatueBlock.FACING);
        float[] rotation = {180, BlockRenderHelper.getRotationFromFacing(facing)};

        /*
         * this is so crappy, but we have to do it :( this is the only path we can use to retrieve the correct
         * texture, but it exposes ".png" and has the "textures/" path in the beginning so we have to manual strip.
         * vanilla does some gymnastics with the passed (render-layer and CopperGolemOxidationLevels) to submitModel
         * inorder to "choose the correct texture" (this happens internally in the immediate pipeline somewhere)
        */
        Identifier texture = CopperGolemOxidationLevels.getOxidationLevel(((CopperGolemStatueBlock)state.getBlock()).getWeatheringState()).texture();
        String strippedFirst = texture.getPath().replace(".png", "");
        String strippedFinal = strippedFirst.replace("textures/", "");
        Identifier strippedTexture = Identifier.withDefaultNamespace(strippedFinal);

        TextureAtlasSprite sprite = QuadTransform.getSprite(strippedTexture);

        helper.setRotation(rotation);
        helper.setSprite(sprite);
        helper.setRendertype(ChunkSectionLayer.SOLID);
        BlockRenderHelper.emitModelPart(merged, emitter, state, isFaceCulled, helper::emitGE);
    }

    /**
     * Safely retrieve this block entity, if we fail, try getting it from the slice data, if fallback fails,
     * abort and skip meshing this block entity. The only reason this should fail is if another mod mutilates
     * the block entity-list. See {@link "net.caffeinemc.mods.sodium.client.world.cloned.ClonedChunkSection#tryCopyBlockEntities"}
     */
    private static @Nullable BlockEntity tryGetBlockEntity(BlockPos pos, BlockAndTintGetter level, LevelSlice slice) {
        try {
            return level.getBlockEntity(pos);
        } catch (Exception e) {
            BBE.getLogger().error("Failed to get block entity at {}. Attempting fallback.", pos, e);
            try {
                return slice.getBlockEntity(pos);
            } catch (Throwable t) {
                BBE.getLogger().error("Fallback failed! This block entity will be skipped and not added to this mesh!", t);
                return null;
            }
        }
    }

    /**
     * if we fail to get this ModelLayerLocation from the registry, schedule a task for registry "rebake",
     * skip meshing, and reload all render sections. this is the best we can do while still being "thread-safe"
     */
    private static Map<String, BlockStateModel> tryGetPairs(ModelLayerLocation location) {
        try {
            MultiPartBlockModel model = (MultiPartBlockModel)GeometryRegistry.getModel(location);
            return model.getPairs();
        } catch (Exception e) {
            TaskScheduler.schedule(() -> {
                if (Tasks.populateGeometryRegistry() == Tasks.TASK_FAILED) {
                    throw new RuntimeException("Failed to repopulate geometry registry after failed location lookup!");
                }
                Tasks.reloadRenderSections();
            });
            return Map.of();
        }
    }

    /**
     * Checks if a BlockEntity should be meshed or not by checking a flag in the vanilla BlockEntity class
     * added by BBE through mixin, this flag is mostly controlled by {@link betterblockentities.client.render.immediate.blockentity.BlockEntityManager}
     * @param ext Extension interface of the vanilla BlockEntity class
     * @return true if the cast from BlockEntity failed or if the extension flag getRemoveChunkVariant is NOT high (true)
     */
    private static boolean shouldRender(BlockEntityExt ext) {
        return ext == null || !ext.getRemoveChunkVariant();
    }
}
