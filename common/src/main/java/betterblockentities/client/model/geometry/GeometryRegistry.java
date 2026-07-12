package betterblockentities.client.model.geometry;

/* local */
import betterblockentities.client.model.texture.SpriteSelector;

/* minecraft */
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.resources.Identifier;

/* mojang */
import com.mojang.blaze3d.vertex.PoseStack;

/* java/misc */
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
