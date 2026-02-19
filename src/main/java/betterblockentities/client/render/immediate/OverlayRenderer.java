package betterblockentities.client.render.immediate;


import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.feature.ModelPartFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;


public final class OverlayRenderer {
    private OverlayRenderer() {}

    public static final int RED   = 0xFFFF0000;
    public static final int GREEN = 0xFF00FF00;
    public static final int BLUE  = 0xFF0000FF;

    public static <S> boolean submitModelOverlay(ModelFeatureRenderer.Storage storage, Model<? super S> model, S object, PoseStack poseStack, int light, int overlayUV, TextureAtlasSprite sprite, int something, ModelFeatureRenderer.CrumblingOverlay crumbling) {
        SubmitNodeStorage.ModelSubmit<S> submit =
                new SubmitNodeStorage.ModelSubmit<>(
                        poseStack.last().copy(),
                        model,
                        object,
                        light,
                        overlayUV,
                        RED,
                        sprite,
                        something,
                        crumbling
                );

        storage.add(RenderTypes.debugFilledBox(), submit);
        return true;
    }

    public static boolean submitModelPartOverlay(ModelPartFeatureRenderer.Storage storage, ModelPart modelPart, PoseStack poseStack, int light, int overlayUV, TextureAtlasSprite sprite, boolean bl, boolean bl2, ModelFeatureRenderer.CrumblingOverlay crumblingOverlay, int l) {
        SubmitNodeStorage.ModelPartSubmit submit =
                new SubmitNodeStorage.ModelPartSubmit(
                        poseStack.last().copy(),
                        modelPart,
                        light,
                        overlayUV,
                        sprite,
                        bl,
                        bl2,
                        RED,
                        crumblingOverlay,
                        l
                );

        storage.add(RenderTypes.debugFilledBox(), submit);
        return true;
    }
}
