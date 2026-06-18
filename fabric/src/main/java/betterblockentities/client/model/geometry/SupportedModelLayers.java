package betterblockentities.client.model.geometry;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;

/**
 * Supported vanilla model layers mapped from the entity model set.
 */
public final class SupportedModelLayers {
    public static final ModelLayerLocation CHEST = ModelLayers.CHEST;
    public static final ModelLayerLocation LEFT_CHEST = ModelLayers.DOUBLE_CHEST_LEFT;
    public static final ModelLayerLocation RIGHT_CHEST = ModelLayers.DOUBLE_CHEST_RIGHT;
    public static final ModelLayerLocation BELL_BODY = ModelLayers.BELL;
    public static final ModelLayerLocation DECORATED_POT_BASE = ModelLayers.DECORATED_POT_BASE;
    public static final ModelLayerLocation DECORATED_POT_SIDES = ModelLayers.DECORATED_POT_SIDES;
    public static final ModelLayerLocation SHULKER = ModelLayers.SHULKER_BOX;
    public static final ModelLayerLocation STANDING_BANNER = ModelLayers.STANDING_BANNER;
    public static final ModelLayerLocation WALL_BANNER = ModelLayers.WALL_BANNER;
    public static final ModelLayerLocation STANDING_BANNER_FLAG = ModelLayers.STANDING_BANNER_FLAG;
    public static final ModelLayerLocation WALL_BANNER_FLAG = ModelLayers.WALL_BANNER_FLAG;
    public static final ModelLayerLocation COPPER_GOLEM = ModelLayers.COPPER_GOLEM;
    public static final ModelLayerLocation COPPER_GOLEM_RUNNING = ModelLayers.COPPER_GOLEM_RUNNING;
    public static final ModelLayerLocation COPPER_GOLEM_SITTING = ModelLayers.COPPER_GOLEM_SITTING;
    public static final ModelLayerLocation COPPER_GOLEM_STAR = ModelLayers.COPPER_GOLEM_STAR;

    public static final ModelLayerLocation[] ALL = {
            CHEST,
            LEFT_CHEST,
            RIGHT_CHEST,
            BELL_BODY,
            DECORATED_POT_BASE,
            DECORATED_POT_SIDES,
            SHULKER,
            STANDING_BANNER,
            WALL_BANNER,
            STANDING_BANNER_FLAG,
            WALL_BANNER_FLAG,
            COPPER_GOLEM,
            COPPER_GOLEM_RUNNING,
            COPPER_GOLEM_SITTING,
            COPPER_GOLEM_STAR
    };
}
