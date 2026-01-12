package betterblockentities.mixin.minecraft.sign;

import betterblockentities.model.BBEGeometryRegistry;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.SignRenderer;
import net.minecraft.client.resources.model.Material;
import net.minecraft.world.level.block.state.properties.WoodType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(SignRenderer.class)
public class SignRendererMixin {
    @Shadow @Final private Map<WoodType, Object> signModels;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void captureSignModels(BlockEntityRendererProvider.Context context, CallbackInfo ci) {
        PoseStack stack = new PoseStack();

        Model.Simple standing = ((SignRendererModelsAccessor)signModels.get(WoodType.OAK)).invokeStanding();
        Model.Simple wall = ((SignRendererModelsAccessor)signModels.get(WoodType.OAK)).invokeWall();

        Material signMaterial = Sheets.getSignMaterial(WoodType.OAK);
        {
            {
                ModelPart sign_standing = standing.root();
                stack.pushPose();
                stack.translate(0.5F, 0.5F, 0.5F);
                stack.scale(0.6666667F, -0.6666667F, -0.6666667F);
                BBEGeometryRegistry.cacheGeometry(BBEGeometryRegistry.BBEModelLayers.SIGN_STANDING, sign_standing, signMaterial.texture(), stack);
                stack.pushPose();
            }

            stack.setIdentity();
            {
                ModelPart sign_wall = wall.root();
                stack.pushPose();
                stack.translate(0.5F, 0.5F, 0.5F);
                stack.translate(0.0F, -0.3125F, -0.4375F);
                stack.scale(0.6666667F, -0.6666667F, -0.6666667F);
                BBEGeometryRegistry.cacheGeometry(BBEGeometryRegistry.BBEModelLayers.SIGN_WALL, sign_wall, signMaterial.texture(), stack);
                stack.pushPose();
            }
        }
    }
}
