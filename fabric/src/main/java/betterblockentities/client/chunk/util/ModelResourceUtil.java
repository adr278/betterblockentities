package betterblockentities.client.chunk.util;

/* local */
import betterblockentities.client.chunk.pipeline.BBEBlockRenderer;
import betterblockentities.client.gui.config.ConfigCache;
import betterblockentities.client.gui.option.EnumTypes;
import betterblockentities.client.model.geometry.GeometryRegistry;
import betterblockentities.client.render.immediate.blockentity.extentions.BlockEntityExt;

/* local */
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.animal.golem.CopperGolemOxidationLevels;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.ChestType;

/* java/misc */
import java.util.ArrayList;
import java.util.Map;

public class ModelResourceUtil {
    public static ModelLayerLocation getChestLayer(BlockState state) {
        if (state.hasProperty(ChestBlock.TYPE)) {
            ChestType type = state.getValue(ChestBlock.TYPE);
            if (type == ChestType.LEFT) {
                return ModelLayers.DOUBLE_CHEST_LEFT;
            }
            else if (type == ChestType.RIGHT) {
                return ModelLayers.DOUBLE_CHEST_RIGHT;
            }
        }
        return ModelLayers.CHEST;
    }

    public static ModelLayerLocation getSignLayer(BlockState state) {
        final boolean isWallSign = !state.hasProperty(BlockStateProperties.ROTATION_16);

        if (isWallSign) {
            return GeometryRegistry.SupportedVanillaModelLayers.SIGN_WALL;
        }
        return GeometryRegistry.SupportedVanillaModelLayers.SIGN_STANDING;
    }

    public static ModelLayerLocation getHangingSignLayer(BlockState state) {
        final boolean isWall = !state.hasProperty(CeilingHangingSignBlock.ATTACHED);
        final boolean attached = !isWall && state.getValue(CeilingHangingSignBlock.ATTACHED);

        if (isWall) {
            return GeometryRegistry.SupportedVanillaModelLayers.HANGING_SIGN_WALL;
        }
        else if (attached) {
            return GeometryRegistry.SupportedVanillaModelLayers.HANGING_SIGN_CEILING_MIDDLE;
        }
        return GeometryRegistry.SupportedVanillaModelLayers.HANGING_SIGN_CEILING;
    }

    public static ModelLayerLocation getBedLayer(BlockState state) {
        if (state.getValue(BedBlock.PART) == BedPart.HEAD) {
            return ModelLayers.BED_HEAD;
        }
        return ModelLayers.BED_FOOT;
    }

    public static ModelLayerLocation getBannerFlagLayer(boolean wall) {
        return wall ? ModelLayers.WALL_BANNER_FLAG : ModelLayers.STANDING_BANNER_FLAG;
    }

    public static ModelLayerLocation getBannerBaseLayer(boolean wall) {
        return wall ? ModelLayers.WALL_BANNER : ModelLayers.STANDING_BANNER;
    }

    public static ModelLayerLocation getCGSLayer(BlockState state) {
        CopperGolemStatueBlock.Pose pose = state.getValue(BlockStateProperties.COPPER_GOLEM_POSE);

        if (pose == CopperGolemStatueBlock.Pose.SITTING) {
            return ModelLayers.COPPER_GOLEM_SITTING;
        }
        else if (pose == CopperGolemStatueBlock.Pose.RUNNING) {
            return ModelLayers.COPPER_GOLEM_RUNNING;
        }
        else if (pose == CopperGolemStatueBlock.Pose.STAR) {
            return ModelLayers.COPPER_GOLEM_STAR;
        }
        return ModelLayers.COPPER_GOLEM;
    }

    public static ModelLayerLocation getShulkerBoxLayer() {
        return ModelLayers.SHULKER_BOX;
    }

    public static ModelLayerLocation getBellLayer() {
        return ModelLayers.BELL;
    }

    public static ModelLayerLocation getDecoratedPotBaseLayer() {
        return ModelLayers.DECORATED_POT_BASE;
    }

    public static ModelLayerLocation getDecoratedPotSideLayer() {
        return ModelLayers.DECORATED_POT_SIDES;
    }

    public static void collectSplitModelParts(BlockEntity blockEntity, ArrayList<BlockStateModelPart> dst, Map<String, BlockStateModel> pairs, RandomSource random) {
        final boolean drawLid = BBEBlockRenderer.shouldRender((BlockEntityExt)blockEntity);
        boolean addBase = true;

        if (!drawLid && ConfigCache.updateType == EnumTypes.UpdateSchedulerType.SMART.ordinal()) {
            addBase = false;
        }

        boolean sw = blockEntity.is(BlockEntityType.SHULKER_BOX);

        if (addBase) {
            collectSingleModelParts(dst, pairs.get(sw ? "base" : "bottom"), random);
        }
        if (drawLid) {
            collectSingleModelParts(dst, pairs.get("lid"), random);
            if (!sw) {
                collectSingleModelParts(dst, pairs.get("lock"), random);
            }
        }
    }

    public static void collectSingleModelParts(ArrayList<BlockStateModelPart> parts, BlockStateModel model, RandomSource random) {
        if (model != null) {
            model.collectParts(random, parts);
        }
    }

    public static void collectMultiModelParts(ArrayList<BlockStateModelPart> parts, Iterable<BlockStateModel> models, RandomSource random) {
        for (BlockStateModel blockModel : models) {
            collectSingleModelParts(parts, blockModel, random);
        }
    }

    public static TextureAtlasSprite getCGSSprite(CopperGolemStatueBlock cgsBlock) {
        final Identifier texture = CopperGolemOxidationLevels.getOxidationLevel(cgsBlock.getWeatheringState()).texture();

        String path = texture.getPath();
        if (path.endsWith(".png")) {
            path = path.substring(0, path.length() - 4);
        }
        if (path.startsWith("textures/")) {
            path = path.substring("textures/".length());
        }

        Identifier strippedTexture = Identifier.withDefaultNamespace(path);
         return QuadTransform.getBlockSprite(strippedTexture);
    }
}
