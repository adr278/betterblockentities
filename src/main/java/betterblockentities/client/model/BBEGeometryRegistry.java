package betterblockentities.client.model;

/* local */
import betterblockentities.client.chunk.util.QuadTransform;

/* minecraft */
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.blockentity.HangingSignRenderer;
import net.minecraft.resources.Identifier;

/* mojang */
import com.mojang.blaze3d.vertex.PoseStack;

/* java/misc */
import java.util.HashMap;
import java.util.Map;

/**
 * A Registry which holds the base geometry for all our supported block entity models, which is then
 * later used when meshing happens in  {@link betterblockentities.client.chunk.pipeline.BBEEmitter}
 * -We append geometry to the registry cache with {@link #cacheGeometry}
 * -Clear the whole registry cache with {@link #clearCache}
 * -And get an entry from the cache with {@link #getModel}
 * Most of the entries to this cache comes from {@link betterblockentities.mixin.model.ModelManagerMixin}
 * but there are unique cases where an entry comes from elsewhere
 */
public class BBEGeometryRegistry {
    private static final Map<ModelLayerLocation, BlockStateModel> cache = new HashMap<>();
    public static Map<HangingSignRenderer.ModelKey, Model.Simple> hangingSignModels = new HashMap<>();

    public static void cacheGeometry(ModelLayerLocation key, ModelPart root, Identifier texture, PoseStack stack) {
        BBEMultiPartModel model = new BBEMultiPartModel(root, QuadTransform.getSprite(texture), stack);
        cache.put(key, model);
    }

    public static BlockStateModel getModel(ModelLayerLocation layer) {
        return cache.get(layer);
    }

    public static void clearCache() {
        cache.clear();
    }

    /* supported vanilla model layers which are mapped inside the entityModelSet */
    public static class SupportedVanillaModelLayers {
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

    /* either totally custom model layers or "vanilla" model layers for geometry that didn't get mapped in the entityModelSet */
    public static class BBEModelLayers {
        public static final ModelLayerLocation SIGN_STANDING;
        public static final ModelLayerLocation SIGN_WALL;
        public static final ModelLayerLocation HANGING_SIGN_CEILING;
        public static final ModelLayerLocation HANGING_SIGN_CEILING_MIDDLE;
        public static final ModelLayerLocation HANGING_SIGN_WALL;
        public static final ModelLayerLocation HANGING_SIGN_CEILING_INVERTED;
        public static final ModelLayerLocation HANGING_SIGN_CEILING_MIDDLE_INVERTED;
        public static final ModelLayerLocation HANGING_SIGN_WALL_INVERTED;

        public static final ModelLayerLocation[] ALL;

        static {
            SIGN_STANDING = new ModelLayerLocation(Identifier.withDefaultNamespace("sign/standing"), "main");
            SIGN_WALL = new ModelLayerLocation(Identifier.withDefaultNamespace("sign/wall"), "main");
            HANGING_SIGN_CEILING = new ModelLayerLocation(Identifier.withDefaultNamespace("hanging_sign/ceiling"), "main");
            HANGING_SIGN_CEILING_MIDDLE = new ModelLayerLocation(Identifier.withDefaultNamespace("hanging_sign/ceiling_middle"), "main");
            HANGING_SIGN_WALL = new ModelLayerLocation(Identifier.withDefaultNamespace("hanging_sign/wall"), "main");
            HANGING_SIGN_CEILING_INVERTED = new ModelLayerLocation(Identifier.withDefaultNamespace("hanging_sign/ceiling_inverted"), "main");
            HANGING_SIGN_CEILING_MIDDLE_INVERTED = new ModelLayerLocation(Identifier.withDefaultNamespace("hanging_sign/ceiling_middle_inverted"), "main");
            HANGING_SIGN_WALL_INVERTED = new ModelLayerLocation(Identifier.withDefaultNamespace("hanging_sign/wall_inverted"), "main");

            ALL = new ModelLayerLocation[]{SIGN_STANDING, SIGN_WALL, HANGING_SIGN_CEILING, HANGING_SIGN_CEILING_MIDDLE, HANGING_SIGN_WALL, HANGING_SIGN_CEILING_INVERTED, HANGING_SIGN_CEILING_MIDDLE_INVERTED, HANGING_SIGN_WALL_INVERTED};
        }
    }
    public static ModelLayerLocation createHangingSignLayer(HangingSignRenderer.AttachmentType attachmentType, boolean chains) {
        Identifier identifier = Identifier.withDefaultNamespace("hanging_sign/" + attachmentType.getSerializedName() + (chains ? "_inverted" : ""));
        return new ModelLayerLocation(identifier, "main");
    }

    /* placeholder sprite identifiers for the model part wrapper */
    public static class PlaceHolderSpriteIdentifiers {
        public static final Identifier CHEST;
        public static final Identifier BELL_BODY;
        public static final Identifier DECORATED_POT_BASE;
        public static final Identifier DECORATED_POT_SIDES;
        public static final Identifier SHULKER;
        public static final Identifier BED_HEAD;
        public static final Identifier BED_FOOT;
        public static final Identifier BANNER;

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
