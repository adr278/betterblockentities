package betterblockentities.chunk;

/* local */
import betterblockentities.BetterBlockEntities;
import betterblockentities.gui.ConfigManager;
import betterblockentities.model.BBEGeometryRegistry;
import betterblockentities.model.BBEMultiPartModel;

/* minecraft */
import betterblockentities.util.BlockEntityExt;
import betterblockentities.util.BlockEntityManager;
import betterblockentities.util.BlockRenderHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;

/* sodium */
import net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderer;
import net.caffeinemc.mods.sodium.client.render.model.MutableQuadViewImpl;
import net.caffeinemc.mods.sodium.client.services.PlatformModelEmitter;

/* java/misc */
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public class BBEEmitter {
    /* handle each block accordingly */
    public static void emit(PlatformModelEmitter instance, BlockStateModel model, Predicate<Direction> isFaceCulled, MutableQuadViewImpl emitter, RandomSource random, BlockAndTintGetter level, BlockPos pos, BlockState state, PlatformModelEmitter.Bufferer bufferer, BlockRenderer blockRenderer) {
        Block block = state.getBlock();
        BlockEntityExt blockEntity = getBlockEntityInstance(pos);

        /* not a valid block (regular terrain or not supported) emit like normal */
        if (blockEntity == null || !BlockEntityManager.isSupportedBlock(block) || !ConfigManager.CONFIG.master_optimize) {
            instance.emitModel(model, isFaceCulled, emitter, random, level, pos, state, bufferer);
            return;
        }

        final BlockRenderHelper helper = new BlockRenderHelper(blockRenderer, (BlockEntity)blockEntity);

        /* NON EMISSIVE CHESTS */
        if (block instanceof ChestBlock) {
            if (ConfigManager.CONFIG.optimize_chests)
                emitChest(isFaceCulled, emitter, random, pos, state, helper, false);
        }

        /* EMISSIVE CHESTS (Ender) */
        else if (block instanceof EnderChestBlock) {
            if (ConfigManager.CONFIG.optimize_chests)
                emitChest(isFaceCulled, emitter, random, pos, state, helper, true);
        }

        /* SHULKER BOX */
        else if (block instanceof ShulkerBoxBlock) {
            if (ConfigManager.CONFIG.optimize_shulkers)
                emitShulker(isFaceCulled, emitter, random, pos, state, helper);
        }

        /* 16 STEP ROTATION SIGNS */
        else if (block instanceof CeilingHangingSignBlock || block instanceof StandingSignBlock) {

        }

        /* CARDINAL SIGNS */
        else if (block instanceof WallSignBlock || block instanceof WallHangingSignBlock) {

        }

        /* BELL */
        else if (block instanceof BellBlock) {
            if (ConfigManager.CONFIG.optimize_bells)
                emitBell(isFaceCulled, emitter, random, pos, state, helper);
        }

        /* DECORATED POT */
        else if (block instanceof DecoratedPotBlock) {
            if (ConfigManager.CONFIG.optimize_decoratedpots)
                emitDecoratedPot(isFaceCulled, emitter, random, pos, state, helper);
        }

        /* BED */
        else if (block instanceof BedBlock) {
            if (ConfigManager.CONFIG.optimize_beds)
                emitBed(isFaceCulled, emitter, random, state, helper);
        }

        /* emit any accessory parts if there are any */
        if (!model.collectParts(random).isEmpty())
            instance.emitModel(model, isFaceCulled, emitter, random, level, pos, state, bufferer);
    }

    private static void emitChest(Predicate<Direction> isFaceCulled, MutableQuadViewImpl emitter, RandomSource random, BlockPos pos, BlockState state, BlockRenderHelper helper, boolean emissive) {
        ModelLayerLocation layer = ModelLayers.CHEST;

        if (!emissive) {
            layer = switch (state.getValue(ChestBlock.TYPE)) {
                case LEFT  -> ModelLayers.DOUBLE_CHEST_LEFT;
                case RIGHT -> ModelLayers.DOUBLE_CHEST_RIGHT;
                default    -> ModelLayers.CHEST;
            };
        }

        Map<String, BlockStateModel> pairs = ((BBEMultiPartModel) BBEGeometryRegistry.cache.get(layer)).getPairs();
        List<BlockModelPart> merged = new ArrayList<>();

        if (ConfigManager.CONFIG.updateType == 1)
            merged.addAll(pairs.get("bottom").collectParts(random));

        if (shouldRender(getBlockEntityInstance(pos))) {
            if (ConfigManager.CONFIG.updateType == 0)
                merged.addAll(pairs.get("bottom").collectParts(random));
            merged.addAll(pairs.get("lid").collectParts(random));
            merged.addAll(pairs.get("lock").collectParts(random));
        }
        BlockRenderHelper.emitModelPart(merged, emitter, state, isFaceCulled, helper::emitChestQuads);
    }

    private static void emitShulker(Predicate<Direction> isFaceCulled, MutableQuadViewImpl emitter, RandomSource random, BlockPos pos, BlockState state, BlockRenderHelper helper) {
        Map<String, BlockStateModel> pairs = ((BBEMultiPartModel) BBEGeometryRegistry.cache.get(ModelLayers.SHULKER_BOX)).getPairs();
        List<BlockModelPart> merged = new ArrayList<>();

        if (ConfigManager.CONFIG.updateType == 1)
            merged.addAll(pairs.get("base").collectParts(random));

        if (shouldRender(getBlockEntityInstance(pos))) {
            if (ConfigManager.CONFIG.updateType == 0)
                merged.addAll(pairs.get("base").collectParts(random));
            merged.addAll(pairs.get("lid").collectParts(random));
        }
        BlockRenderHelper.emitModelPart(merged, emitter, state, isFaceCulled, helper::emitShulkerQuads);
    }

    private static void emitBell(Predicate<Direction> isFaceCulled, MutableQuadViewImpl emitter, RandomSource random, BlockPos pos, BlockState state, BlockRenderHelper helper) {
        if (!shouldRender(getBlockEntityInstance(pos))) return;

        Map<String, BlockStateModel> pairs = ((BBEMultiPartModel) BBEGeometryRegistry.cache.get(ModelLayers.BELL)).getPairs();
        List<BlockModelPart> bellBodyParts = pairs.get("bell_body").collectParts(random);
        BlockRenderHelper.emitModelPart(bellBodyParts, emitter, state, isFaceCulled, helper::emitBellQuads);
    }

    private static void emitBed(Predicate<Direction> isFaceCulled, MutableQuadViewImpl emitter, RandomSource random, BlockState state, BlockRenderHelper helper) {
        ModelLayerLocation layer = state.getValue(BedBlock.PART) == BedPart.HEAD ? ModelLayers.BED_HEAD : ModelLayers.BED_FOOT;

        Map<String, BlockStateModel> pairs = ((BBEMultiPartModel) BBEGeometryRegistry.cache.get(layer)).getPairs();

        List<BlockModelPart> merged = new ArrayList<>();
        for (String name : new String[]{"main", "right_leg", "left_leg"})
            merged.addAll(pairs.get(name).collectParts(random));
        BlockRenderHelper.emitModelPart(merged, emitter, state, isFaceCulled, helper::emitBedQuads);
    }

    private static void emitDecoratedPot(Predicate<Direction> isFaceCulled, MutableQuadViewImpl emitter, RandomSource random, BlockPos pos, BlockState state, BlockRenderHelper helper) {
        if (!shouldRender(getBlockEntityInstance(pos))) return;

        ModelLayerLocation[] layers = { ModelLayers.DECORATED_POT_BASE, ModelLayers.DECORATED_POT_SIDES };

        Map<String, BlockStateModel> basePairs  = ((BBEMultiPartModel) BBEGeometryRegistry.cache.get(layers[0])).getPairs();
        Map<String, BlockStateModel> sidePairs  = ((BBEMultiPartModel) BBEGeometryRegistry.cache.get(layers[1])).getPairs();

        List<BlockModelPart> merged = new ArrayList<>();
        java.util.function.BiConsumer<Map<String, BlockStateModel>, String[]> addParts = (pairs, names) -> {
            for (String n : names) merged.addAll(pairs.get(n).collectParts(random));
        };

        addParts.accept(basePairs, new String[]{"top", "neck", "bottom"});
        addParts.accept(sidePairs, new String[]{"left", "right", "back", "front"});

        BlockRenderHelper.emitModelPart(merged, emitter, state, isFaceCulled, helper::emitDecoratedPotQuads);
    }

    /* get an instance to our accessor */
    private static BlockEntityExt getBlockEntityInstance(BlockPos pos) {
        try {
            ClientLevel world = Minecraft.getInstance().level;
            BlockEntity blockEntity = world.getBlockEntity(pos);
            return (blockEntity instanceof BlockEntityExt bex) ? bex : null;
        } catch (Exception e) {
            BetterBlockEntities.getLogger().error("Error: Getting Block Entity and accessor at {}", pos, e);
            return null;
        }
    }

    /* should we emit this block entity into this mesh  */
    private static boolean shouldRender(BlockEntityExt ext) {
        return ext == null || !ext.getRemoveChunkVariant();
    }
}
