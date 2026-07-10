package betterblockentities.client.model.geometry;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.world.level.block.HangingSignBlock;
import net.minecraft.world.level.block.state.properties.WoodType;

public final class SupportedModelLayers {
    public static final ModelLayerLocation CHEST = ModelLayers.CHEST;
    public static final ModelLayerLocation LEFT_CHEST = ModelLayers.DOUBLE_CHEST_LEFT;
    public static final ModelLayerLocation RIGHT_CHEST = ModelLayers.DOUBLE_CHEST_RIGHT;
    public static final ModelLayerLocation BELL_BODY = ModelLayers.BELL;
    public static final ModelLayerLocation DECORATED_POT_BASE = ModelLayers.DECORATED_POT_BASE;
    public static final ModelLayerLocation DECORATED_POT_SIDES = ModelLayers.DECORATED_POT_SIDES;
    public static final ModelLayerLocation SHULKER = ModelLayers.SHULKER_BOX;
    public static final ModelLayerLocation BED_HEAD = ModelLayers.BED_HEAD;
    public static final ModelLayerLocation BED_FOOT = ModelLayers.BED_FOOT;
    public static final ModelLayerLocation STANDING_BANNER = ModelLayers.STANDING_BANNER;
    public static final ModelLayerLocation WALL_BANNER = ModelLayers.WALL_BANNER;
    public static final ModelLayerLocation STANDING_BANNER_FLAG = ModelLayers.STANDING_BANNER_FLAG;
    public static final ModelLayerLocation WALL_BANNER_FLAG = ModelLayers.WALL_BANNER_FLAG;
    public static final ModelLayerLocation SIGN_WALL = ModelLayers.createWallSignModelName(WoodType.OAK);
    public static final ModelLayerLocation SIGN_STANDING = ModelLayers.createStandingSignModelName(WoodType.OAK);
    public static final ModelLayerLocation HANGING_SIGN_WALL = ModelLayers.createHangingSignModelName(WoodType.OAK, HangingSignBlock.Attachment.WALL);
    public static final ModelLayerLocation HANGING_SIGN_CEILING = ModelLayers.createHangingSignModelName(WoodType.OAK, HangingSignBlock.Attachment.CEILING);
    public static final ModelLayerLocation HANGING_SIGN_CEILING_MIDDLE = ModelLayers.createHangingSignModelName(WoodType.OAK, HangingSignBlock.Attachment.CEILING_MIDDLE);
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
            BED_HEAD,
            BED_FOOT,
            STANDING_BANNER,
            WALL_BANNER,
            STANDING_BANNER_FLAG,
            WALL_BANNER_FLAG,
            SIGN_WALL,
            SIGN_STANDING,
            HANGING_SIGN_WALL,
            HANGING_SIGN_CEILING,
            HANGING_SIGN_CEILING_MIDDLE,
            COPPER_GOLEM,
            COPPER_GOLEM_RUNNING,
            COPPER_GOLEM_SITTING,
            COPPER_GOLEM_STAR
    };
}