package betterblockentities.mixin.render.immediate.blockentity.sign;

import betterblockentities.client.model.BBEGeometryRegistry;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.HangingSignRenderer;
import net.minecraft.client.resources.model.Material;
import net.minecraft.world.level.block.state.properties.WoodType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.Map;

@Mixin(HangingSignRenderer.class)
public class HangingSignRendererMixin {
    @Shadow @Final private Map<HangingSignRenderer.ModelKey, Model.Simple> hangingSignModels;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void captureHangingSignModels(BlockEntityRendererProvider.Context context, CallbackInfo ci) {
        PoseStack stack = new PoseStack();
        Material signMaterial = Sheets.getSignMaterial(WoodType.OAK);

        for (HangingSignRenderer.AttachmentType type : HangingSignRenderer.AttachmentType.values()) {
            HangingSignRenderer.ModelKey key = new HangingSignRenderer.ModelKey(WoodType.OAK, type);
            ModelPart root = this.hangingSignModels.get(key).root();

            stack.pushPose();
            stack.translate(0.5, 0.9375, 0.5);
            stack.mulPose(Axis.XP.rotationDegrees(180.0F));
            stack.translate(0.0F, 0.3125F, 0.0F);
            BBEGeometryRegistry.cacheGeometry(BBEGeometryRegistry.createHangingSignLayer(type, false), root, signMaterial.texture(), stack);
            stack.popPose();
        }

        stack.setIdentity();
        for (HangingSignRenderer.AttachmentType type : HangingSignRenderer.AttachmentType.values()) {
            HangingSignRenderer.ModelKey key = new HangingSignRenderer.ModelKey(WoodType.OAK, type);
            ModelPart root = this.hangingSignModels.get(key).root();

            stack.pushPose();
            stack.translate(0.5, 0.9375, 0.5);
            stack.mulPose(Axis.XP.rotationDegrees(180.0F));
            stack.mulPose(Axis.YP.rotationDegrees(180.0F));
            stack.translate(0.0F, 0.3125F , 0.0F);
            BBEGeometryRegistry.cacheGeometry(BBEGeometryRegistry.createHangingSignLayer(type, true), root, signMaterial.texture(), stack);
            stack.popPose();
        }
    }
}
