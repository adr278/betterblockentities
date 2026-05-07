package betterblockentities.client.model.geometry;

/* local */
import betterblockentities.client.chunk.util.QuadTransform;
import betterblockentities.client.model.MultiPartBlockModel;

/* minecraft */
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.blockentity.HangingSignRenderer;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.resources.Identifier;

/* mojang */
import com.mojang.blaze3d.vertex.PoseStack;

/* java/misc */
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A Registry which holds the base geometry for all our supported block entity models, which is then
 * later used when meshing happens in {@link betterblockentities.client.chunk.pipeline.BBEEmitter}
 * -We append geometry to the registry cache with {@link #cacheGeometry}
 * -Clear the whole registry cache with {@link #clearCache}
 * -And get an entry from the cache with {@link #getModel}
 */
public final class GeometryRegistry {
    private static final ConcurrentHashMap<ModelLayerLocation, BlockStateModel> CACHE = new ConcurrentHashMap<>();

    public static void cacheGeometry(ModelLayerLocation key, ModelPart root, Identifier texture, PoseStack stack) {
        CACHE.put(key, new MultiPartBlockModel(root, QuadTransform.getBlockSprite(texture), stack));
    }

    public static BlockStateModel getModel(ModelLayerLocation layer) {
        return CACHE.get(layer);
    }

    public static void clearCache() {
        CACHE.clear();
    }

    public static Map<ModelLayerLocation, BlockStateModel> getCache() {
        return CACHE;
    }

    /**
     * Supported vanilla model layers mapped from the entity model set.
     */
    public static final class SupportedVanillaModelLayers {
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
        public static final ModelLayerLocation HANGING_SIGN_WALL = ModelLayers.createHangingSignModelName(WoodType.OAK, HangingSignRenderer.AttachmentType.WALL);
        public static final ModelLayerLocation HANGING_SIGN_CEILING = ModelLayers.createHangingSignModelName(WoodType.OAK, HangingSignRenderer.AttachmentType.CEILING);
        public static final ModelLayerLocation HANGING_SIGN_CEILING_MIDDLE = ModelLayers.createHangingSignModelName(WoodType.OAK, HangingSignRenderer.AttachmentType.CEILING_MIDDLE);
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

    /**
     * Placeholder sprite identifiers used by the model part wrapper.
     */
    public static final class PlaceHolderSpriteIdentifiers {
        public static final Identifier CHEST = Identifier.withDefaultNamespace("entity/chest/normal");
        public static final Identifier BELL_BODY = Identifier.withDefaultNamespace("entity/bell/bell_body");
        public static final Identifier DECORATED_POT_BASE = Identifier.withDefaultNamespace("entity/decorated_pot/decorated_pot_base");
        public static final Identifier DECORATED_POT_SIDES = Identifier.withDefaultNamespace("entity/decorated_pot/decorated_pot_side");
        public static final Identifier SHULKER = Identifier.withDefaultNamespace("entity/shulker/shulker");
        public static final Identifier BED_HEAD = Identifier.withDefaultNamespace("entity/bed/bed_head");
        public static final Identifier BED_FOOT = Identifier.withDefaultNamespace("entity/bed/bed_foot");
        public static final Identifier BANNER = Identifier.withDefaultNamespace("entity/banner_base");
        public static final Identifier SIGN = Sheets.getSignMaterial(WoodType.OAK).texture();
        public static final Identifier HANGING_SIGN = Sheets.getHangingSignMaterial(WoodType.OAK).texture();
        public static final Identifier COPPER_GOLEM_STATUE = Identifier.withDefaultNamespace("entity/copper_golem/copper_golem");

        public static final Identifier[] ALL = {
                CHEST,
                BELL_BODY,
                DECORATED_POT_BASE,
                DECORATED_POT_SIDES,
                SHULKER,
                BED_HEAD,
                BED_FOOT,
                BANNER,
                SIGN,
                HANGING_SIGN,
                COPPER_GOLEM_STATUE
        };
    }
}
