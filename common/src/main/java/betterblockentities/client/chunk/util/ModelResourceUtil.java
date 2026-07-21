package betterblockentities.client.chunk.util;

/* minecraft */
import betterblockentities.platform.GlobalScope;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.Material;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.BannerPattern;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.DecoratedPotPattern;
import net.minecraft.world.level.block.entity.DecoratedPotPatterns;
import net.minecraft.world.level.block.entity.EnderChestBlockEntity;
import net.minecraft.world.level.block.entity.TrappedChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.block.state.properties.WoodType;

/* fabric */
import net.fabricmc.fabric.api.renderer.v1.Renderer;
import net.fabricmc.fabric.api.renderer.v1.RendererAccess;
import net.fabricmc.fabric.api.renderer.v1.material.BlendMode;
import net.fabricmc.fabric.api.renderer.v1.material.MaterialFinder;
import net.fabricmc.fabric.api.renderer.v1.material.RenderMaterial;

/* java/misc */
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class ModelResourceUtil {
    private static final int VANILLA_DYE_COLOR_COUNT = 16;
    private static final ResourceLocation BED_SHEET = vanilla("textures/atlas/beds.png");
    private static final ResourceLocation BANNER_SHEET = vanilla("textures/atlas/banner_patterns.png");
    private static final ResourceLocation SHIELD_SHEET = vanilla("textures/atlas/shield_patterns.png");
    private static final ResourceLocation SHULKER_SHEET = vanilla("textures/atlas/shulker_boxes.png");
    private static final ResourceLocation SIGN_SHEET = vanilla("textures/atlas/signs.png");
    private static final ResourceLocation CHEST_SHEET = vanilla("textures/atlas/chest.png");
    private static final ResourceLocation DECORATED_POT_SHEET = vanilla("textures/atlas/decorated_pot.png");

    private static final Material DEFAULT_SHULKER_MATERIAL = new Material(SHULKER_SHEET, vanilla("entity/shulker/shulker"));
    private static final Material BANNER_BASE_MATERIAL = new Material(BANNER_SHEET, vanilla("entity/banner/base"));
    private static final Material SHIELD_BASE_MATERIAL = new Material(SHIELD_SHEET, vanilla("entity/shield/base"));
    private static final Material DECORATED_POT_BASE_MATERIAL = createDecoratedPotMaterial(vanilla("decorated_pot_base"));
    private static final Material DECORATED_POT_SIDE_MATERIAL = createDecoratedPotMaterial(vanilla("decorated_pot_side"));

    private static final Material NORMAL_CHEST_MATERIAL = createChestMaterial("normal");
    private static final Material NORMAL_CHEST_LEFT_MATERIAL = createChestMaterial("normal_left");
    private static final Material NORMAL_CHEST_RIGHT_MATERIAL = createChestMaterial("normal_right");
    private static final Material TRAPPED_CHEST_MATERIAL = createChestMaterial("trapped");
    private static final Material TRAPPED_CHEST_LEFT_MATERIAL = createChestMaterial("trapped_left");
    private static final Material TRAPPED_CHEST_RIGHT_MATERIAL = createChestMaterial("trapped_right");
    private static final Material CHRISTMAS_CHEST_MATERIAL = createChestMaterial("christmas");
    private static final Material CHRISTMAS_CHEST_LEFT_MATERIAL = createChestMaterial("christmas_left");
    private static final Material CHRISTMAS_CHEST_RIGHT_MATERIAL = createChestMaterial("christmas_right");
    private static final Material ENDER_CHEST_MATERIAL = createChestMaterial("ender");

    private static final Map<BlendMode, RenderMaterial> MATERIALS = buildMaterials();
    private static final Map<ResourceLocation, Material> BANNER_PATTERN_MATERIALS = new ConcurrentHashMap<>();
    private static final Map<ResourceLocation, Material> SHIELD_PATTERN_MATERIALS = new ConcurrentHashMap<>();
    private static final Map<ResourceLocation, Material> DECORATED_POT_MATERIALS = new ConcurrentHashMap<>();
    private static final Map<String, Material> EXTENDED_BED_MATERIALS = new ConcurrentHashMap<>();
    private static final Map<String, Material> EXTENDED_SHULKER_MATERIALS = new ConcurrentHashMap<>();
    private static final Set<Material> MISSING_MATERIAL_SPRITES = ConcurrentHashMap.newKeySet();
    private static final Set<ResourceLocation> MISSING_TEXTURE_SPRITES = ConcurrentHashMap.newKeySet();

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

    public static ModelLayerLocation getSignLayer() {
        return ModelLayers.createSignModelName(WoodType.OAK);
    }

    public static ModelLayerLocation getHangingSignLayer() {
        return ModelLayers.createHangingSignModelName(WoodType.OAK);
    }

    public static ModelLayerLocation getBedLayer(final BlockState state) {
        return state.getValue(BedBlock.PART) == BedPart.HEAD ? ModelLayers.BED_HEAD : ModelLayers.BED_FOOT;
    }

    public static Material getBedMaterial(final BlockState state, final DyeColor color) {
        final int colorId = color.getId();
        if (colorId >= 0 && colorId < VANILLA_DYE_COLOR_COUNT) {
            return createBedMaterial(ResourceLocation.DEFAULT_NAMESPACE, color.getSerializedName());
        }

        final String colorName = color.getSerializedName();
        final String blockNamespace = BuiltInRegistries.BLOCK.getKey(state.getBlock()).getNamespace();
        final Material blockMaterial = createBedMaterial(blockNamespace, colorName);
        if (hasTextureResource(blockMaterial.texture())) {
            return blockMaterial;
        }

        return EXTENDED_BED_MATERIALS.computeIfAbsent(colorName, ignored -> resolveExtendedBedMaterial(blockMaterial, colorName));
    }

    public static ModelLayerLocation getBannerLayer() {
        return ModelLayers.BANNER;
    }

    public static ModelLayerLocation getShulkerBoxLayer() {
        return ModelLayers.SHULKER;
    }

    public static Material getShulkerMaterial(final BlockState state, final DyeColor color) {
        if (color == null) {
            return DEFAULT_SHULKER_MATERIAL;
        }

        final int colorId = color.getId();
        if (colorId >= 0 && colorId < VANILLA_DYE_COLOR_COUNT) {
            return createShulkerMaterial(ResourceLocation.DEFAULT_NAMESPACE, color.getSerializedName());
        }

        final String colorName = color.getSerializedName();
        final String blockNamespace = BuiltInRegistries.BLOCK.getKey(state.getBlock()).getNamespace();
        final Material blockMaterial = createShulkerMaterial(blockNamespace, colorName);
        if (hasTextureResource(blockMaterial.texture())) {
            return blockMaterial;
        }

        return EXTENDED_SHULKER_MATERIALS.computeIfAbsent(colorName, ignored -> resolveExtendedShulkerMaterial(blockMaterial, colorName));
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

    public static Material getChestMaterial(final BlockEntity blockEntity, final ChestType chestType, final boolean christmas) {
        if (blockEntity instanceof EnderChestBlockEntity) {
            return ENDER_CHEST_MATERIAL;
        }

        if (christmas) {
            return selectChestMaterial(chestType, CHRISTMAS_CHEST_MATERIAL, CHRISTMAS_CHEST_LEFT_MATERIAL, CHRISTMAS_CHEST_RIGHT_MATERIAL);
        }

        if (blockEntity instanceof TrappedChestBlockEntity) {
            return selectChestMaterial(chestType, TRAPPED_CHEST_MATERIAL, TRAPPED_CHEST_LEFT_MATERIAL, TRAPPED_CHEST_RIGHT_MATERIAL);
        }

        return selectChestMaterial(chestType, NORMAL_CHEST_MATERIAL, NORMAL_CHEST_LEFT_MATERIAL, NORMAL_CHEST_RIGHT_MATERIAL);
    }

    public static Material getBannerBaseMaterial(final boolean banner) {
        return banner ? BANNER_BASE_MATERIAL : SHIELD_BASE_MATERIAL;
    }

    public static Material getBannerPatternMaterial(final Holder<BannerPattern> pattern, final boolean banner) {
        final ResourceLocation assetId = pattern.value().assetId();
        final Map<ResourceLocation, Material> materials = banner ? BANNER_PATTERN_MATERIALS : SHIELD_PATTERN_MATERIALS;
        final ResourceLocation sheet = banner ? BANNER_SHEET : SHIELD_SHEET;
        final String prefix = banner ? "entity/banner/" : "entity/shield/";
        return materials.computeIfAbsent(assetId, id -> new Material(sheet, id.withPrefix(prefix)));
    }

    public static Material getDecoratedPotBaseMaterial() {
        return DECORATED_POT_BASE_MATERIAL;
    }

    public static Material getPotSideMaterial(final Optional<Item> decorationItem) {
        if (decorationItem.isPresent()) {
            final var patternKey = DecoratedPotPatterns.getPatternFromItem(decorationItem.get());
            if (patternKey != null) {
                final Optional<Holder.Reference<DecoratedPotPattern>> pattern = BuiltInRegistries.DECORATED_POT_PATTERN.getHolder(patternKey);
                if (pattern.isPresent()) {
                    final ResourceLocation assetId = pattern.get().value().assetId();
                    return DECORATED_POT_MATERIALS.computeIfAbsent(assetId, ModelResourceUtil::createDecoratedPotMaterial);
                }
            }
        }
        return DECORATED_POT_SIDE_MATERIAL;
    }

    public static Material getSignMaterial(final WoodType woodType) {
        return new Material(SIGN_SHEET, textureLocation(ResourceLocation.DEFAULT_NAMESPACE, woodType.name(), "entity/signs/"));
    }

    public static Material getHangingSignMaterial(final WoodType woodType) {
        return new Material(SIGN_SHEET, textureLocation(ResourceLocation.DEFAULT_NAMESPACE, woodType.name(), "entity/signs/hanging/"));
    }

    public static TextureAtlasSprite spriteForBannerPattern(final Holder<BannerPattern> pattern) {
        try {
            return spriteForTexture(pattern.value().assetId().withPrefix("entity/banner/"));
        } catch (Exception ignored) {
            return null;
        }
    }

    public static TextureAtlasSprite spriteForMaterial(final Material material) {
        try {
            final TextureAtlasSprite sprite = spriteForTexture(material.texture());
            if (sprite.contents().name().equals(MissingTextureAtlasSprite.getLocation()) && MISSING_MATERIAL_SPRITES.add(material)) {
                GlobalScope.LOGGER.warn(
                        "Missing material sprite for texture {} from atlas {}",
                        material.texture(),
                        material.atlasLocation()
                );
            }
            return sprite;
        } catch (Exception ignored) {
            return null;
        }
    }

    public static TextureAtlasSprite spriteForTexture(final ResourceLocation texture) {
        final TextureAtlasSprite sprite = Minecraft.getInstance()
                .getTextureAtlas(TextureAtlas.LOCATION_BLOCKS)
                .apply(texture);
        if (sprite.contents().name().equals(MissingTextureAtlasSprite.getLocation()) && MISSING_TEXTURE_SPRITES.add(texture)) {
            GlobalScope.LOGGER.warn("Missing sprite for texture {} from atlas {}", texture, TextureAtlas.LOCATION_BLOCKS);
        }
        return sprite;
    }

    public static RenderMaterial toMaterial(final BlendMode blendMode) {
        final RenderMaterial material = MATERIALS.get(blendMode);
        if (material != null) {
            return material;
        }
        return MATERIALS.get(BlendMode.DEFAULT);
    }

    private static Map<BlendMode, RenderMaterial> buildMaterials() {
        final Map<BlendMode, RenderMaterial> materials = new EnumMap<>(BlendMode.class);
        final Renderer renderer = RendererAccess.INSTANCE.getRenderer();
        if (renderer == null) {
            return materials;
        }

        final MaterialFinder finder = renderer.materialFinder();
        for (BlendMode mode : BlendMode.values()) {
            materials.put(mode, finder.clear().blendMode(mode).find());
        }
        return materials;
    }

    private static Material resolveExtendedBedMaterial(final Material fallback, final String colorName) {
        for (String namespace : Minecraft.getInstance().getResourceManager().getNamespaces()) {
            final Material candidate = createBedMaterial(namespace, colorName);
            if (hasTextureResource(candidate.texture())) {
                return candidate;
            }
        }

        return fallback;
    }

    private static Material createBedMaterial(final String namespace, final String colorName) {
        return new Material(
                BED_SHEET,
                textureLocation(namespace, colorName, "entity/bed/")
        );
    }

    private static Material resolveExtendedShulkerMaterial(final Material fallback, final String colorName) {
        for (String namespace : Minecraft.getInstance().getResourceManager().getNamespaces()) {
            final Material candidate = createShulkerMaterial(namespace, colorName);
            if (hasTextureResource(candidate.texture())) {
                return candidate;
            }
        }

        return fallback;
    }

    private static Material createShulkerMaterial(final String namespace, final String colorName) {
        return new Material(
                SHULKER_SHEET,
                textureLocation(namespace, colorName, "entity/shulker/shulker_")
        );
    }

    private static Material createChestMaterial(final String textureName) {
        return new Material(CHEST_SHEET, textureLocation(ResourceLocation.DEFAULT_NAMESPACE, textureName, "entity/chest/"));
    }

    private static Material selectChestMaterial(
            final ChestType chestType,
            final Material single,
            final Material left,
            final Material right
    ) {
        return switch (chestType) {
            case LEFT -> left;
            case RIGHT -> right;
            default -> single;
        };
    }

    private static Material createDecoratedPotMaterial(final ResourceLocation assetId) {
        return new Material(DECORATED_POT_SHEET, assetId.withPrefix("entity/decorated_pot/"));
    }

    private static ResourceLocation vanilla(final String path) {
        return ResourceLocation.withDefaultNamespace(path);
    }

    private static ResourceLocation textureLocation(final String defaultNamespace, final String name, final String pathPrefix) {
        final ResourceLocation assetId = name.indexOf(':') >= 0
                ? ResourceLocation.parse(name)
                : ResourceLocation.fromNamespaceAndPath(defaultNamespace, name);
        return assetId.withPrefix(pathPrefix);
    }

    private static boolean hasTextureResource(final ResourceLocation texture) {
        final ResourceLocation textureFile = texture.withPrefix("textures/").withSuffix(".png");
        return Minecraft.getInstance().getResourceManager().getResource(textureFile).isPresent();
    }
}
