package betterblockentities.chunk;

/* local */
import betterblockentities.BetterBlockEntities;
import betterblockentities.gui.ConfigManager;
import betterblockentities.mixin.minecraft.chest.ChestBlockEntityRendererAccessor;
import betterblockentities.model.BBEGeometryRegistry;
import betterblockentities.model.BBEMultiPartModel;
import betterblockentities.util.BlockEntityExt;
import betterblockentities.util.BlockEntityManager;

/* minecraft */
import net.caffeinemc.mods.sodium.client.services.PlatformRuntimeInformation;
import net.caffeinemc.mods.sodium.client.world.LevelSlice;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.model.*;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.state.ChestRenderState;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BannerBlockEntity;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.block.state.properties.WoodType;

/* sodium */
import net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderer;
import net.caffeinemc.mods.sodium.client.render.model.MutableQuadViewImpl;
import net.caffeinemc.mods.sodium.client.services.PlatformModelEmitter;
import org.jetbrains.annotations.Nullable;

/* java/misc */
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class BBEEmitter {
    private static final BlockEntityRenderDispatcher dispatcher = Minecraft.getInstance().getBlockEntityRenderDispatcher();

    /* handle each block accordingly */
    public static void emit(PlatformModelEmitter instance, BlockStateModel model, Predicate<Direction> isFaceCulled, MutableQuadViewImpl emitter, RandomSource random, BlockAndTintGetter level, LevelSlice slice, BlockPos pos, BlockState state, PlatformModelEmitter.Bufferer bufferer, BlockRenderer blockRenderer) {
        Block block = state.getBlock();

        /* not a valid block (regular terrain or not supported) emit like normal */
        if (!BlockEntityManager.isSupportedBlock(block) || !ConfigManager.CONFIG.master_optimize) {
            instance.emitModel(model, isFaceCulled, emitter, random, level, pos, state, bufferer);
            return;
        }

        /* invalid block entity, abort */
        BlockEntity blockEntity = tryGetBlockEntity(pos, level, slice);
        if (blockEntity == null) {
            return;
        }

        final BlockRenderHelper helper = new BlockRenderHelper(blockRenderer, blockEntity);

        /* NON EMISSIVE CHESTS */
        if (block instanceof ChestBlock) {
            if (ConfigManager.CONFIG.optimize_chests)
                emitChest(isFaceCulled, emitter, random, pos, state, helper, false, blockEntity);
        }

        /* EMISSIVE CHESTS (Ender) */
        else if (block instanceof EnderChestBlock) {
            if (ConfigManager.CONFIG.optimize_chests)
                emitChest(isFaceCulled, emitter, random, pos, state, helper, true, blockEntity);
        }

        /* SHULKER BOX */
        else if (block instanceof ShulkerBoxBlock) {
            if (ConfigManager.CONFIG.optimize_shulkers)
                emitShulker(isFaceCulled, emitter, random, pos, state, helper, blockEntity);
        }

        /* 16 STEP ROTATION SIGNS */
        else if (block instanceof CeilingHangingSignBlock || block instanceof WallHangingSignBlock) {
            if (ConfigManager.CONFIG.optimize_signs)
                emitHangingSign(isFaceCulled, emitter, random, pos, state, helper);
        }

        /* CARDINAL SIGNS */
        else if (block instanceof WallSignBlock || block instanceof StandingSignBlock) {
            if (ConfigManager.CONFIG.optimize_signs)
                emitSign(isFaceCulled, emitter, random, pos, state, helper);
        }

        /* BELL */
        else if (block instanceof BellBlock) {
            if (ConfigManager.CONFIG.optimize_bells)
                emitBell(isFaceCulled, emitter, random, pos, state, helper, blockEntity);
        }

        /* DECORATED POT */
        else if (block instanceof DecoratedPotBlock) {
            if (ConfigManager.CONFIG.optimize_decoratedpots)
                emitDecoratedPot(isFaceCulled, emitter, random, pos, state, helper, blockEntity);
        }

        /* BED */
        else if (block instanceof BedBlock) {
            if (ConfigManager.CONFIG.optimize_beds)
                emitBed(isFaceCulled, emitter, random, state, helper);
        }

        /* BANNERS */
        else if (block instanceof BannerBlock || block instanceof WallBannerBlock) {
            if (ConfigManager.CONFIG.optimize_banners)
                emitBanner(isFaceCulled, emitter, random, pos, state, helper, blockEntity);
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

        Map<String, BlockStateModel> pairs = ((BBEMultiPartModel) BBEGeometryRegistry.getModel(layer)).getPairs();


        boolean drawLid = shouldRender((BlockEntityExt)blockEntity);
        boolean addBase  = ConfigManager.CONFIG.updateType == 1 || (drawLid && ConfigManager.CONFIG.updateType == 0);
        List<BlockModelPart> merged = new ArrayList<>();
        if (addBase) merged.addAll(pairs.get("bottom").collectParts(random));
        if (drawLid) {
            merged.addAll(pairs.get("lid").collectParts(random));
            merged.addAll(pairs.get("lock").collectParts(random));
        }

        ChestBlockEntityRendererAccessor ber = (ChestBlockEntityRendererAccessor)dispatcher.getRenderer(blockEntity);
        boolean christmas = ConfigManager.CONFIG.chest_christmas;
        ChestRenderState.ChestMaterialType ChestMaterial = ber.getChestMaterialInvoke(blockEntity, christmas);
        ChestType type = state.hasProperty(ChestBlock.TYPE) ? (ChestType)state.getValue(ChestBlock.TYPE) : ChestType.SINGLE;
        Material material = Sheets.chooseMaterial(ChestMaterial, type);

        helper.setMaterial(material);
        helper.setRendertype(ChunkSectionLayer.SOLID);
        BlockRenderHelper.emitModelPart(merged, emitter, state, isFaceCulled, helper::emitGE);
    }

    private static void emitShulker(Predicate<Direction> isFaceCulled, MutableQuadViewImpl emitter, RandomSource random, BlockPos pos, BlockState state, BlockRenderHelper helper, BlockEntity blockEntity) {
        Map<String, BlockStateModel> pairs = ((BBEMultiPartModel) BBEGeometryRegistry.getModel(ModelLayers.SHULKER_BOX)).getPairs();

        boolean drawLid = shouldRender((BlockEntityExt)blockEntity);
        boolean addBase  = ConfigManager.CONFIG.updateType == 1 || (drawLid && ConfigManager.CONFIG.updateType == 0);
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

        ModelLayerLocation layerLocation = isWallSign ? BBEGeometryRegistry.BBEModelLayers.SIGN_WALL : BBEGeometryRegistry.BBEModelLayers.SIGN_STANDING;

        Map<String, BlockStateModel> pairs = ((BBEMultiPartModel) BBEGeometryRegistry.getModel(layerLocation)).getPairs();
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

        ModelLayerLocation layerLocation1 = isWall ? BBEGeometryRegistry.BBEModelLayers.HANGING_SIGN_WALL
                : attached ? BBEGeometryRegistry.BBEModelLayers.HANGING_SIGN_CEILING_MIDDLE : BBEGeometryRegistry.BBEModelLayers.HANGING_SIGN_CEILING;
        ModelLayerLocation layerLocation2 = isWall ? BBEGeometryRegistry.BBEModelLayers.HANGING_SIGN_WALL_INVERTED
                : attached ? BBEGeometryRegistry.BBEModelLayers.HANGING_SIGN_CEILING_MIDDLE_INVERTED : BBEGeometryRegistry.BBEModelLayers.HANGING_SIGN_CEILING_INVERTED;

        Map<String, BlockStateModel> pairs = ((BBEMultiPartModel) BBEGeometryRegistry.getModel(layerLocation1)).getPairs();
        Map<String, BlockStateModel> pairs2 = ((BBEMultiPartModel) BBEGeometryRegistry.getModel(layerLocation2)).getPairs();

        List<BlockModelPart> merged = new java.util.ArrayList<>(
                pairs.values().stream().flatMap(m -> m.collectParts(random).stream()).toList()
        );

        /* backface culling fix for chains... vanilla chains "parts" are only one quad thick, so we need to double render them but inverted */
        BlockStateModel chains = pairs2.get(attached ? "vChains" : "normalChains");
        if (chains != null) merged.addAll(chains.collectParts(random));

        WoodType woodType = ((SignBlock)state.getBlock()).type();
        Material signMaterial = Sheets.getHangingSignMaterial(woodType);

        helper.setMaterial(signMaterial);
        helper.setRendertype(ChunkSectionLayer.CUTOUT);
        BlockRenderHelper.emitModelPart(merged, emitter, state, isFaceCulled, helper::emitGE);
    }

    private static void emitBell(Predicate<Direction> isFaceCulled, MutableQuadViewImpl emitter, RandomSource random, BlockPos pos, BlockState state, BlockRenderHelper helper, BlockEntity blockEntity) {
        if (!shouldRender((BlockEntityExt)blockEntity)) return;

        Map<String, BlockStateModel> pairs = ((BBEMultiPartModel) BBEGeometryRegistry.getModel(ModelLayers.BELL)).getPairs();
        List<BlockModelPart> bellBodyParts = pairs.get("bell_body").collectParts(random);

        Material bellBodyMaterial = Sheets.BLOCK_ENTITIES_MAPPER.defaultNamespaceApply("bell/bell_body");

        helper.setMaterial(bellBodyMaterial);
        helper.setRendertype(ChunkSectionLayer.SOLID);
        BlockRenderHelper.emitModelPart(bellBodyParts, emitter, state, isFaceCulled, helper::emitGE);
    }

    private static void emitBed(Predicate<Direction> isFaceCulled, MutableQuadViewImpl emitter, RandomSource random, BlockState state, BlockRenderHelper helper) {
        ModelLayerLocation layer = state.getValue(BedBlock.PART) == BedPart.HEAD ? ModelLayers.BED_HEAD : ModelLayers.BED_FOOT;

        Map<String, BlockStateModel> pairs = ((BBEMultiPartModel) BBEGeometryRegistry.getModel(layer)).getPairs();
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

        Map<String, BlockStateModel> basePairs  = ((BBEMultiPartModel) BBEGeometryRegistry.getModel(layers[0])).getPairs();
        Map<String, BlockStateModel> sidePairs  = ((BBEMultiPartModel) BBEGeometryRegistry.getModel(layers[1])).getPairs();

        List<BlockModelPart> merged = Stream.concat(basePairs.values().stream(), sidePairs.values().stream())
                .flatMap(model -> model.collectParts(random).stream())
                .toList();

        BlockRenderHelper.emitModelPart(merged, emitter, state, isFaceCulled, helper::emitDecoratedPotQuads);
    }

    private static void emitBanner(Predicate<Direction> isFaceCulled, MutableQuadViewImpl emitter, RandomSource random, BlockPos pos, BlockState state, BlockRenderHelper helper, BlockEntity blockEntity) {
        BannerBlockEntity bannerBlockEntity = (BannerBlockEntity)blockEntity;

        boolean isWallBanner = !state.hasProperty(BlockStateProperties.ROTATION_16);
        ModelLayerLocation layerLocation = isWallBanner ? ModelLayers.WALL_BANNER : ModelLayers.STANDING_BANNER;
        ModelLayerLocation layerLocation2 = isWallBanner ? ModelLayers.WALL_BANNER_FLAG : ModelLayers.STANDING_BANNER_FLAG;

        Map<String, BlockStateModel> basePairs  = ((BBEMultiPartModel) BBEGeometryRegistry.getModel(layerLocation)).getPairs();
        Map<String, BlockStateModel> canvasPairs  = ((BBEMultiPartModel) BBEGeometryRegistry.getModel(layerLocation2)).getPairs();

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
            helper.setRendertype(ConfigManager.CONFIG.bannerLayer == 0 ? ChunkSectionLayer.TRANSLUCENT : ChunkSectionLayer.CUTOUT);
            helper.setColor(layerColor.getTextureDiffuseColor());
            BlockRenderHelper.emitModelPart(canvasParts, emitter, state, isFaceCulled, helper::emitGE);
        }
    }

    /* safely retrieve this block entity, if we fail, try getting it from the slice data, if fallback fails, abort and skip meshing this block entity */
    private static @Nullable BlockEntity tryGetBlockEntity(BlockPos pos, BlockAndTintGetter level, LevelSlice slice) {
        try {
            return level.getBlockEntity(pos);
        } catch (Exception e) {
            BetterBlockEntities.getLogger().error("Failed to get block entity at {}. Attempting fallback.", pos, e);
            try {
                return slice.getBlockEntity(pos);
            } catch (Throwable t) {
                BetterBlockEntities.getLogger().error("Fallback failed! This block entity will be skipped and not added to this mesh!", t);
                return null;
            }
        }
    }

    /* should we emit this block entity into this mesh  */
    private static boolean shouldRender(BlockEntityExt ext) {
        return ext == null || !ext.getRemoveChunkVariant();
    }
}
