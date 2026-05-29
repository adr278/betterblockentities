package betterblockentities.client.model.geometry;

/* local */
import betterblockentities.client.BBE;
import betterblockentities.client.gui.config.ConfigCache;
import betterblockentities.client.chunk.util.ModelResourceUtil;
import betterblockentities.client.tasks.ResourceTasks;

/* minecraft */
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/* mojang */
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.level.block.state.properties.WoodType;

/* util */
import java.util.ArrayList;
import java.util.List;

/**
 * Builds the pre-baked geometry cache from entity model layers.
 */
public final class ModelGenerator {
    private ModelGenerator() {
    }

    public static int generateAppend() {
        final EntityModelSet entityModelSet = tryGetEntityModelSet();
        if (entityModelSet == null) {
            return ResourceTasks.FAILED;
        }

        final PoseStack stack = new PoseStack();
        final List<ModelLayerLocation> layers = new ArrayList<>();

        layers.add(ModelLayers.CHEST);
        layers.add(ModelLayers.DOUBLE_CHEST_LEFT);
        layers.add(ModelLayers.DOUBLE_CHEST_RIGHT);
        layers.add(ModelResourceUtil.getShulkerBoxLayer());
        layers.add(ModelResourceUtil.getBellLayer());
        layers.add(ModelLayers.BED_HEAD);
        layers.add(ModelLayers.BED_FOOT);
        layers.add(ModelResourceUtil.getDecoratedPotBaseLayer());
        layers.add(ModelResourceUtil.getDecoratedPotSideLayer());
        layers.add(ModelResourceUtil.getBannerLayer());
        WoodType.values().forEach((woodType) -> {
            layers.add(ModelLayers.createSignModelName(woodType));
            layers.add(ModelLayers.createHangingSignModelName(woodType));
        });

        for (ModelLayerLocation layer : layers) {
            try {
                final ModelPart root = entityModelSet.bakeLayer(layer);
                if (layer == ModelLayers.BANNER) {
                    final ModelPart flag = root.getChild("flag");
                    final float step = -0.45F;
                    final float rotation = step * ConfigCache.bannerPose;
                    final float clamped = Mth.clamp(rotation, -4.05F, -0.45F);
                    flag.xRot = (float) Math.toRadians(clamped);
                    flag.y = -32.0F;
                }
                stack.setIdentity();
                GeometryRegistry.cacheGeometry(layer, root, stack);
                logUvRange(layer);
            } catch (Exception e) {
                BBE.getLogger().error("Failed baking geometry layer {}", layer, e);
            }
        }

        return ResourceTasks.COMPLETE;
    }

    private static void logUvRange(final ModelLayerLocation layer) {
        final GeometryRegistry.ModelTemplate template = GeometryRegistry.getModel(layer);
        if (template == null || template.quads().isEmpty()) {
            return;
        }

        float minU = Float.POSITIVE_INFINITY;
        float minV = Float.POSITIVE_INFINITY;
        float maxU = Float.NEGATIVE_INFINITY;
        float maxV = Float.NEGATIVE_INFINITY;

        for (GeometryRegistry.QuadTemplate quad : template.quads()) {
            final float[] uv = quad.uvs();
            for (int i = 0; i < uv.length; i += 2) {
                minU = Math.min(minU, uv[i]);
                minV = Math.min(minV, uv[i + 1]);
                maxU = Math.max(maxU, uv[i]);
                maxV = Math.max(maxV, uv[i + 1]);
            }
        }

        BBE.getLogger().info("Geometry UV range {} -> u:[{}, {}] v:[{}, {}]", layer, minU, maxU, minV, maxV);
    }

    private static EntityModelSet tryGetEntityModelSet() {
        try {
            return Minecraft.getInstance().getEntityModels();
        } catch (Exception e) {
            BBE.getLogger().error("Failed to get EntityModelSet while caching geometry", e);
            return null;
        }
    }
}
