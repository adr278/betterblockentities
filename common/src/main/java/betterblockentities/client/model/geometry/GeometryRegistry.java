package betterblockentities.client.model.geometry;

/* minecraft */
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.properties.WoodType;

/* mojang */
import com.mojang.blaze3d.vertex.PoseStack;


/* java/misc */
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Stores pre-baked model-part geometry templates used by the Sodium terrain BE pipeline.
 */
public final class GeometryRegistry {
    private static final ConcurrentHashMap<ModelLayerLocation, MultiPartBlockModel> CACHE = new ConcurrentHashMap<>();

    public static void cacheGeometry(ModelLayerLocation key, ModelPart root, ResourceLocation texture, PoseStack stack) {
        final TextureAtlasSprite sprite = Minecraft.getInstance()
                .getTextureAtlas(TextureAtlas.LOCATION_BLOCKS)
                .apply(texture);

        CACHE.put(key, new MultiPartBlockModel(root, sprite, stack));
    }

    public static MultiPartBlockModel getModel(ModelLayerLocation layer) {
        return CACHE.get(layer);
    }

    public static void clearCache() {
        CACHE.clear();
    }

    public static Map<ModelLayerLocation, MultiPartBlockModel> getCache() {
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
        public static final ModelLayerLocation SHULKER = ModelLayers.SHULKER;
        public static final ModelLayerLocation BED_HEAD = ModelLayers.BED_HEAD;
        public static final ModelLayerLocation BED_FOOT = ModelLayers.BED_FOOT;
        public static final ModelLayerLocation BANNER = ModelLayers.BANNER;
        public static final ModelLayerLocation SIGN = ModelLayers.createSignModelName(WoodType.OAK);
        public static final ModelLayerLocation HANGING_SIGN = ModelLayers.createHangingSignModelName(WoodType.OAK);

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
                BANNER,
                SIGN,
                HANGING_SIGN
        };
    }

    /**
     * Placeholder sprite identifiers used by the model part wrapper.
     */
    public static final class PlaceHolderSpriteIdentifiers {
        public static final ResourceLocation CHEST = ResourceLocation.withDefaultNamespace("entity/chest/normal");
        public static final ResourceLocation BELL_BODY = ResourceLocation.withDefaultNamespace("entity/bell/bell_body");
        public static final ResourceLocation DECORATED_POT_BASE = ResourceLocation.withDefaultNamespace("entity/decorated_pot/decorated_pot_base");
        public static final ResourceLocation DECORATED_POT_SIDES = ResourceLocation.withDefaultNamespace("entity/decorated_pot/decorated_pot_side");
        public static final ResourceLocation SHULKER = ResourceLocation.withDefaultNamespace("entity/shulker/shulker");
        public static final ResourceLocation BED_HEAD = ResourceLocation.withDefaultNamespace("entity/bed/bed_head");
        public static final ResourceLocation BED_FOOT = ResourceLocation.withDefaultNamespace("entity/bed/bed_foot");
        public static final ResourceLocation BANNER = ResourceLocation.withDefaultNamespace("entity/banner_base");
        public static final ResourceLocation SIGN = ResourceLocation.withDefaultNamespace("entity/signs/oak");
        public static final ResourceLocation HANGING_SIGN = ResourceLocation.withDefaultNamespace("entity/signs/hanging/oak");
        public static final ResourceLocation COPPER_GOLEM_STATUE = ResourceLocation.withDefaultNamespace("entity/copper_golem/copper_golem");

        public static final ResourceLocation[] ALL = {
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
