package betterblockentities.client.chunk.util;

/* minecraft */
import betterblockentities.platform.GlobalScope;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.Material;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.DecoratedPotPatterns;
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
    private static final Map<BlendMode, RenderMaterial> MATERIALS = buildMaterials();
    private static final Set<Material> MISSING_MATERIAL_SPRITES = ConcurrentHashMap.newKeySet();

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

   public static Material getPotSideMaterial(final Optional<Item> decorationItem) {
        if (decorationItem.isPresent()) {
            final Material material = Sheets.getDecoratedPotMaterial(DecoratedPotPatterns.getPatternFromItem(decorationItem.get()));
            if (material != null) {
                return material;
            }
        }
        return Sheets.DECORATED_POT_SIDE;
    }

    public static TextureAtlasSprite spriteForMaterial(final Material material) {
        try {
            final TextureAtlasSprite sprite = Minecraft.getInstance()
                    .getTextureAtlas(TextureAtlas.LOCATION_BLOCKS)
                    .apply(material.texture());
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
}
