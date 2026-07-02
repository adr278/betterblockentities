package betterblockentities.client.model.geometry;

/* local */
import betterblockentities.client.gui.config.ConfigCache;
import betterblockentities.client.tasks.ResourceTasks;

/* minecraft */
import betterblockentities.platform.GlobalScope;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;

/* mojang */
import com.mojang.blaze3d.vertex.PoseStack;

/**
 * Builds the pre-baked geometry cache from entity model layers.
 */
public class ModelGenerator {
    public static int generateAppend() {
        PoseStack stack = new PoseStack();

        EntityModelSet entityModelSet = tryGetEntityModelSet();
        if (entityModelSet == null) {
            return ResourceTasks.FAILED;
        }

        /* iterate all supported layers, bake root, transform, build and append geometry to registry */
        for (ModelLayerLocation layer : GeometryRegistry.SupportedVanillaModelLayers.ALL) {
            try {
                bakeLayerSetupAndAppend(entityModelSet, layer, stack);
            }
            catch (Exception e) {
                GlobalScope.LOGGER.error("Geometry setup for ModelLayer {} failed!", layer.getLayer(), e);
            }
        }
        return ResourceTasks.COMPLETE;
    }

    public static void bakeLayerSetupAndAppend(EntityModelSet entityModelSet, ModelLayerLocation layer, PoseStack stack) {
        ModelPart root = entityModelSet.bakeLayer(layer);
        if (root.getAllParts().toList().isEmpty()) {
            GlobalScope.LOGGER.error("Root ModelPart for ModelLayer {} is empty after bake! Skipping", layer.getLayer());
            return;
        }

        /* reset stack per layer iteration */
        stack.setIdentity();

        if (layer == ModelLayers.SHULKER) {
            setupShulker(layer, root, stack);
        }

        else if (layer == ModelLayers.DOUBLE_CHEST_RIGHT  ||
                layer == ModelLayers.DOUBLE_CHEST_LEFT   ||
                layer == ModelLayers.CHEST) {
            setupChest(layer, root, stack);
        }

        else if (layer == ModelLayers.BELL) {
            setupBell(layer, root, stack);
        }

        else if (layer == ModelLayers.BED_HEAD ||
                layer == ModelLayers.BED_FOOT) {
            setupBed(layer, root, stack);
        }

        else if (layer == ModelLayers.DECORATED_POT_BASE   ||
                layer == ModelLayers.DECORATED_POT_SIDES) {
            setupDecoratedPot(layer, root, stack);
        }

        else if (layer == ModelLayers.BANNER) {
            setupBanners(layer, root, stack);
        }

        else if (layer == GeometryRegistry.SupportedVanillaModelLayers.SIGN) {
            setupSigns(layer, root, stack);
        }

        else if (layer == GeometryRegistry.SupportedVanillaModelLayers.HANGING_SIGN) {
            setupHangingSigns(layer, root, stack);
        }
    }

    private static void setupShulker(ModelLayerLocation layer, ModelPart root, PoseStack stack) {
        GeometryRegistry.cacheGeometry(layer, root, GeometryRegistry.PlaceHolderSpriteIdentifiers.SHULKER, stack);
    }

    private static void setupChest(ModelLayerLocation layer, ModelPart root, PoseStack stack) {
        GeometryRegistry.cacheGeometry(layer, root, GeometryRegistry.PlaceHolderSpriteIdentifiers.CHEST, stack);
    }

    private static void setupBell(ModelLayerLocation layer, ModelPart root, PoseStack stack) {
        GeometryRegistry.cacheGeometry(layer, root, GeometryRegistry.PlaceHolderSpriteIdentifiers.BELL_BODY, stack);
    }

    private static void setupBed(ModelLayerLocation layer, ModelPart root, PoseStack stack) {
        GeometryRegistry.cacheGeometry(layer, root,
                layer == ModelLayers.BED_HEAD ?
                        GeometryRegistry.PlaceHolderSpriteIdentifiers.BED_HEAD :
                        GeometryRegistry.PlaceHolderSpriteIdentifiers.BED_FOOT,
                stack);
    }

    private static void setupDecoratedPot(ModelLayerLocation layer, ModelPart root, PoseStack stack) {
        GeometryRegistry.cacheGeometry(layer, root, GeometryRegistry.PlaceHolderSpriteIdentifiers.DECORATED_POT_BASE, stack);
    }

    private static void setupBanners(ModelLayerLocation layer, ModelPart root, PoseStack stack) {
        ModelPart flag = root.getChild("flag");

        float step = -0.45f;
        float rot = step * ConfigCache.bannerPose;
        float rotClamped = Math.clamp(rot, -4.05f, -0.45f);
        flag.xRot = (float)Math.toRadians(rotClamped);
        flag.y = -32.0F;

        GeometryRegistry.cacheGeometry(layer, root, GeometryRegistry.PlaceHolderSpriteIdentifiers.BANNER, stack);
    }

    private static void setupSigns(ModelLayerLocation layer, ModelPart root, PoseStack stack) {
        GeometryRegistry.cacheGeometry(layer, root, GeometryRegistry.PlaceHolderSpriteIdentifiers.SIGN, stack);
    }

    private static void setupHangingSigns(ModelLayerLocation layer, ModelPart root, PoseStack stack) {
        GeometryRegistry.cacheGeometry(layer, root, GeometryRegistry.PlaceHolderSpriteIdentifiers.HANGING_SIGN, stack);
    }

    private static EntityModelSet tryGetEntityModelSet() {
        try {
            return Minecraft.getInstance().getEntityModels();
        } catch (Exception e) {
            GlobalScope.LOGGER.error("Failed to get EntityModelSet while caching block entity geometry!", e);
            return null;
        }
    }
}
