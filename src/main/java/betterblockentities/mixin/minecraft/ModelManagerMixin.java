package betterblockentities.mixin.minecraft;

/* local */
import betterblockentities.model.BBEGeometryRegistry;

/* minecraft */
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.HangingSignRenderer;
import net.minecraft.client.renderer.blockentity.SignRenderer;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelManager;

/* mojang */
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

/* mixin */
import net.minecraft.world.level.block.state.properties.WoodType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ModelManager.class)
public class ModelManagerMixin {
    @Inject(method = "apply", at = @At("TAIL"))
    private void prepareBBEGeometry(CallbackInfo ci) {
        EntityModelSet entityModelSet = Minecraft.getInstance().getEntityModels();
        PoseStack stack = new PoseStack();

        /*
            signs are a special case because they don't register or declare a ModelLayerLocation we can use
            these are handled separately by mixins into their renderer-constructors where models are created
        */
        for (ModelLayerLocation layer : BBEGeometryRegistry.SupportedVanillaModelLayers.ALL) {
            ModelPart root = entityModelSet.bakeLayer(layer);

            stack.setIdentity();

            if (layer == ModelLayers.SHULKER_BOX) {
                stack.pushPose();
                stack.translate(0.5F, -0.5F, 0.5F);
                stack.mulPose(Axis.YP.rotationDegrees(180.0F));
                BBEGeometryRegistry.cacheGeometry(layer, root, BBEGeometryRegistry.PlaceHolderSpriteIdentifiers.SHULKER, stack);
                stack.popPose();
            } else if (layer == ModelLayers.CHEST) {
                stack.pushPose();
                stack.translate(0.5F, 0.0F, 0.5F);
                BBEGeometryRegistry.cacheGeometry(layer, root, BBEGeometryRegistry.PlaceHolderSpriteIdentifiers.CHEST, stack);
                stack.popPose();
            } else if (layer == ModelLayers.DOUBLE_CHEST_RIGHT || layer == ModelLayers.DOUBLE_CHEST_LEFT) {
                stack.pushPose();
                stack.translate(0.5F, 0.5F, 0.5F);
                stack.mulPose(Axis.YP.rotationDegrees(-0));
                stack.translate(-0.5F, -0.5F, -0.5F);
                BBEGeometryRegistry.cacheGeometry(layer, root, BBEGeometryRegistry.PlaceHolderSpriteIdentifiers.CHEST, stack);
                stack.popPose();
            } else if (layer == ModelLayers.BELL) {
                BBEGeometryRegistry.cacheGeometry(layer, root, BBEGeometryRegistry.PlaceHolderSpriteIdentifiers.BELL_BODY, stack);
            } else if (layer == ModelLayers.BED_HEAD || layer == ModelLayers.BED_FOOT) {
                stack.pushPose();
                stack.translate(0.0F, 0.5625F, 0.0F);
                stack.mulPose(Axis.XP.rotationDegrees(90.0F));
                stack.translate(0.5F, 0.5F, 0.5F);
                stack.mulPose(Axis.ZP.rotationDegrees(180.0F));
                stack.translate(-0.5F, -0.5F, -0.5F);
                BBEGeometryRegistry.cacheGeometry(layer, root,
                        layer == ModelLayers.BED_HEAD ?
                                BBEGeometryRegistry.PlaceHolderSpriteIdentifiers.BED_HEAD :
                                BBEGeometryRegistry.PlaceHolderSpriteIdentifiers.BED_FOOT,
                        stack);
                stack.popPose();
            } else if (layer == ModelLayers.DECORATED_POT_BASE) {
                stack.pushPose();
                stack.translate(0.5F, 0.0F, 0.5F);
                stack.mulPose(Axis.YP.rotationDegrees(180.0F));
                stack.translate(-0.5F, 0.0F, -0.5F);
                BBEGeometryRegistry.cacheGeometry(layer, root, BBEGeometryRegistry.PlaceHolderSpriteIdentifiers.DECORATED_POT_BASE, stack);
                stack.popPose();
            } else if (layer == ModelLayers.DECORATED_POT_SIDES) {
                stack.pushPose();
                stack.translate(0.5F, 0.0F, 0.5F);
                stack.mulPose(Axis.YP.rotationDegrees(180.0F));
                stack.translate(-0.5F, 0.0F, -0.5F);
                BBEGeometryRegistry.cacheGeometry(layer, root, BBEGeometryRegistry.PlaceHolderSpriteIdentifiers.DECORATED_POT_SIDES, stack);
                stack.popPose();
            } else if (layer == ModelLayers.STANDING_BANNER ||
                    layer == ModelLayers.WALL_BANNER ||
                    layer == ModelLayers.STANDING_BANNER_FLAG ||
                    layer == ModelLayers.WALL_BANNER_FLAG) {
                stack.pushPose();
                stack.translate(0.5F, 0.0F, 0.5F);
                stack.scale(0.6666667F, -0.6666667F, -0.6666667F);
                BBEGeometryRegistry.cacheGeometry(layer, root, BBEGeometryRegistry.PlaceHolderSpriteIdentifiers.BANNER, stack);
                stack.popPose();
            }
        }
    }
}
