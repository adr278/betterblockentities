package betterblockentities.mixin.minecraft;

/* local */
import betterblockentities.model.BBEGeometryRegistry;

/* minecraft */
import com.google.common.collect.ImmutableMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.renderer.blockentity.HangingSignRenderer;
import net.minecraft.client.renderer.blockentity.SignRenderer;
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

        for (ModelLayerLocation layer : BBEGeometryRegistry.SupportedModelLayers.ALL) {
            ModelPart root = entityModelSet.bakeLayer(layer);

            if (layer == ModelLayers.SHULKER_BOX) {
                stack.pushPose();
                stack.translate(0.5F, -0.5F, 0.5F);
                stack.mulPose(Axis.YP.rotationDegrees(180.0F));
                BBEGeometryRegistry.cacheGeometry(layer, root, BBEGeometryRegistry.PlaceHolderSpriteIdentifiers.SHULKER, stack);
                stack.popPose();
            } else if (layer == ModelLayers.CHEST || layer == ModelLayers.DOUBLE_CHEST_RIGHT || layer == ModelLayers.DOUBLE_CHEST_LEFT) {
                BBEGeometryRegistry.cacheGeometry(layer, root, BBEGeometryRegistry.PlaceHolderSpriteIdentifiers.CHEST, stack);
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
                stack.translate(0.5F,0.0F,0.5F);
                stack.mulPose(Axis.YP.rotationDegrees(180.0F));
                stack.translate(-0.5F,0.0F,-0.5F);
                BBEGeometryRegistry.cacheGeometry(layer, root, BBEGeometryRegistry.PlaceHolderSpriteIdentifiers.DECORATED_POT_BASE, stack);
                stack.popPose();
            } else if (layer == ModelLayers.DECORATED_POT_SIDES) {
                stack.pushPose();
                stack.translate(0.5F,0.0F,0.5F);
                stack.mulPose(Axis.YP.rotationDegrees(180.0F));
                stack.translate(-0.5F,0.0F,-0.5F);
                BBEGeometryRegistry.cacheGeometry(layer, root, BBEGeometryRegistry.PlaceHolderSpriteIdentifiers.DECORATED_POT_SIDES, stack);
                stack.popPose();
            } else if (layer == ModelLayers.STANDING_BANNER      ||
                       layer == ModelLayers.WALL_BANNER          ||
                       layer == ModelLayers.STANDING_BANNER_FLAG ||
                       layer == ModelLayers.WALL_BANNER_FLAG) {
                BBEGeometryRegistry.cacheGeometry(layer, root, BBEGeometryRegistry.PlaceHolderSpriteIdentifiers.BANNER, stack);
            }
        }
    }
}
