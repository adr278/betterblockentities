package betterblockentities.client.chunk.util;

/* minecraft */
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.SignBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.block.state.properties.WoodType;

public final class ModelResourceUtil {

    public static ModelLayerLocation getChestLayer(final BlockState state) {
        if (state.hasProperty(ChestBlock.TYPE)) {
            final ChestType type = state.getValue(ChestBlock.TYPE);
            if (type == ChestType.LEFT) {
                return ModelLayers.DOUBLE_CHEST_LEFT;
            }
            if (type == ChestType.RIGHT) {
                return ModelLayers.DOUBLE_CHEST_RIGHT;
            }
        }
        return ModelLayers.CHEST;
    }

    public static ModelLayerLocation getSignLayer(final BlockState state) {
        if (state.getBlock() instanceof SignBlock signBlock) {
            return ModelLayers.createSignModelName(signBlock.type());
        }

        return ModelLayers.createSignModelName(WoodType.OAK);
    }

    public static ModelLayerLocation getHangingSignLayer(final BlockState state) {
        if (state.getBlock() instanceof SignBlock signBlock) {
            return ModelLayers.createHangingSignModelName(signBlock.type());
        }

        return ModelLayers.createHangingSignModelName(WoodType.OAK);
    }

    public static ModelLayerLocation getBedLayer(final BlockState state) {
        return state.getValue(BedBlock.PART) == BedPart.HEAD ? ModelLayers.BED_HEAD : ModelLayers.BED_FOOT;
    }

    public static ModelLayerLocation getBannerLayer() {
        return ModelLayers.BANNER;
    }

    public static ModelLayerLocation getShulkerBoxLayer() {
        return ModelLayers.SHULKER;
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
}
