package betterblockentities.model;

/* local */
import betterblockentities.util.ModelTransform;

/* minecraft */
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.resources.Identifier;

/* mojang */
import com.mojang.blaze3d.vertex.PoseStack;

/* java/misc */
import java.util.HashMap;
import java.util.Map;

public class BBEGeometryRegistry {
    /* fastutil here is unnecessary as these only gets accessed on ModelManager reload */
    public static Map<ModelLayerLocation, BlockStateModel> cache = new HashMap<>();

    public static void cacheGeometry(ModelLayerLocation key, ModelPart root, Identifier texture, PoseStack stack) {
        BBEMultiPartModel model = new BBEMultiPartModel(root, ModelTransform.getSprite(texture), stack);
        BBEGeometryRegistry.cache.put(key, model);
    }

    public static class SupportedModelLayers {
        public static final ModelLayerLocation CHEST;
        public static final ModelLayerLocation LEFT_CHEST;
        public static final ModelLayerLocation RIGHT_CHEST;
        public static final ModelLayerLocation BELL_BODY;
        public static final ModelLayerLocation DECORATED_POT_BASE;
        public static final ModelLayerLocation DECORATED_POT_SIDES;
        public static final ModelLayerLocation SHULKER;
        public static final ModelLayerLocation BED_HEAD;
        public static final ModelLayerLocation BED_FOOT;
        public static final ModelLayerLocation STANDING_BANNER;
        public static final ModelLayerLocation WALL_BANNER;
        public static final ModelLayerLocation STANDING_BANNER_FLAG;
        public static final ModelLayerLocation WALL_BANNER_FLAG;

        public static final ModelLayerLocation[] ALL;

        static {
            CHEST = ModelLayers.CHEST;
            LEFT_CHEST = ModelLayers.DOUBLE_CHEST_LEFT;
            RIGHT_CHEST = ModelLayers.DOUBLE_CHEST_RIGHT;
            BELL_BODY = ModelLayers.BELL;
            DECORATED_POT_BASE = ModelLayers.DECORATED_POT_BASE;
            DECORATED_POT_SIDES = ModelLayers.DECORATED_POT_SIDES;
            SHULKER = ModelLayers.SHULKER_BOX;
            BED_HEAD = ModelLayers.BED_HEAD;
            BED_FOOT = ModelLayers.BED_FOOT;
            STANDING_BANNER = ModelLayers.STANDING_BANNER;
            WALL_BANNER = ModelLayers.WALL_BANNER;
            STANDING_BANNER_FLAG = ModelLayers.STANDING_BANNER_FLAG;
            WALL_BANNER_FLAG = ModelLayers.WALL_BANNER_FLAG;

            ALL = new ModelLayerLocation[]{CHEST, LEFT_CHEST, RIGHT_CHEST, BELL_BODY, DECORATED_POT_BASE, DECORATED_POT_SIDES, SHULKER, BED_HEAD, BED_FOOT, STANDING_BANNER, WALL_BANNER, STANDING_BANNER_FLAG, WALL_BANNER_FLAG};
        }
    }

    public static class PlaceHolderSpriteIdentifiers {
        public static final Identifier CHEST;
        public static final Identifier BELL_BODY;
        public static final Identifier DECORATED_POT_BASE;
        public static final Identifier DECORATED_POT_SIDES;
        public static final Identifier SHULKER;
        public static final Identifier BED_HEAD;
        public static final Identifier BED_FOOT;
        public static final Identifier BANNER;;

        public static final Identifier[] ALL;

        static {
            CHEST = Identifier.withDefaultNamespace("entity/chest/normal");
            BELL_BODY = Identifier.withDefaultNamespace("entity/bell/bell_body");
            DECORATED_POT_BASE = Identifier.withDefaultNamespace("entity/decorated_pot/decorated_pot_base");
            DECORATED_POT_SIDES = Identifier.withDefaultNamespace("entity/decorated_pot/decorated_pot_side");
            SHULKER = Identifier.withDefaultNamespace("entity/shulker/shulker");
            BED_HEAD = Identifier.withDefaultNamespace("entity/bed/bed_head");
            BED_FOOT = Identifier.withDefaultNamespace("entity/bed/bed_foot");
            BANNER = Identifier.withDefaultNamespace("entity/banner_base");

            ALL = new Identifier[]{CHEST, BELL_BODY, DECORATED_POT_BASE, DECORATED_POT_SIDES, SHULKER, BED_HEAD, BED_FOOT, BANNER};
        }
    }
}
