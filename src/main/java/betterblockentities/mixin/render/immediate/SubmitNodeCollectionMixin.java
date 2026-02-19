package betterblockentities.mixin.render.immediate;

/* minecraft */
import betterblockentities.client.gui.config.ConfigCache;
import betterblockentities.client.render.immediate.OverlayRenderer;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.feature.ModelPartFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

/* mojang */
import com.mojang.blaze3d.vertex.PoseStack;

/* mixin */
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SubmitNodeCollection.class)
public class SubmitNodeCollectionMixin {
    @Shadow private boolean wasUsed;
    @Shadow @Final private ModelFeatureRenderer.Storage modelSubmits;
    @Shadow @Final private ModelPartFeatureRenderer.Storage modelPartSubmits;

    @Inject(method = "submitModel", at = @At("HEAD"), cancellable = true)
    private <S> void submitModelOverlay(Model<? super S> model, S object, PoseStack poseStack, RenderType renderType, int light, int overlayUV, int color, TextureAtlasSprite sprite, int something, ModelFeatureRenderer.CrumblingOverlay crumbling, CallbackInfo ci) {
        if (!ConfigCache.debugOverlays) return;

        ci.cancel();

        this.wasUsed = OverlayRenderer.submitModelOverlay(this.modelSubmits, model, object, poseStack, light, overlayUV, sprite, something, crumbling);
    }

    @Inject(method = "submitModelPart", at = @At("HEAD"), cancellable = true)
    public void submitModelPartOverlay(ModelPart modelPart, PoseStack poseStack, RenderType renderType, int light, int overlayUV, TextureAtlasSprite sprite, boolean bl, boolean bl2, int color, ModelFeatureRenderer.CrumblingOverlay crumblingOverlay, int l, CallbackInfo ci) {
        if (!ConfigCache.debugOverlays) return;

        ci.cancel();

        this.wasUsed = OverlayRenderer.submitModelPartOverlay(this.modelPartSubmits, modelPart, poseStack, light, overlayUV, sprite, bl, bl2, crumblingOverlay, l);
    }
}
