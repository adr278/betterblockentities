package betterblockentities.client.model.geometry;

/* local */
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.ChestType;

/* java/misc */
import java.util.ArrayList;

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
}
