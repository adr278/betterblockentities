package betterblockentities.client.model.geometry;

/* local */
import betterblockentities.client.chunk.pipeline.BBEBlockRenderer;
import betterblockentities.client.chunk.util.QuadTransform;

/* minecraft */
import betterblockentities.client.model.texture.SpriteSelector;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.resources.Identifier;

/* mojang */
import com.mojang.blaze3d.vertex.PoseStack;

/* java/misc */
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A Registry which holds the base geometry for all our supported block entity models, which is then
 * later used when meshing happens in {@link BBEBlockRenderer}
 * -We append geometry to the registry cache with {@link #cacheGeometry}
 * -Clear the whole registry cache with {@link #clearCache}
 * -And get an entry from the cache with {@link #getModel}
 */
public final class GeometryRegistry {
    private static final ConcurrentHashMap<ModelLayerLocation, BlockStateModel> CACHE = new ConcurrentHashMap<>();

    public static void cacheGeometry(ModelLayerLocation key, ModelPart root, Identifier texture, PoseStack stack) {
        CACHE.put(key, new MultiPartBlockModel(root, SpriteSelector.getBlockSprite(texture), stack));
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
}
