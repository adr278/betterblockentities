package betterblockentities.mixin.render.immediate.blockentity.banner;

/* local */
import betterblockentities.client.gui.config.ConfigCache;
import betterblockentities.client.render.immediate.OverlayRenderer;
import betterblockentities.client.render.immediate.blockentity.BlockEntityRenderStateExt;

/* minecraft */
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BannerRenderer;
import net.minecraft.client.renderer.blockentity.state.BannerRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.MaterialSet;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.entity.BannerBlockEntity;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import net.minecraft.world.phys.Vec3;

/* mojang */
import com.mojang.blaze3d.vertex.PoseStack;

/* mixin */
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BannerRenderer.class)
public abstract class BannerRendererMixin {
    @Unique private static final ThreadLocal<BannerRenderState> RENDER_STATE = new ThreadLocal<>();

    @Inject(method = "extractRenderState(Lnet/minecraft/world/level/block/entity/BannerBlockEntity;Lnet/minecraft/client/renderer/blockentity/state/BannerRenderState;FLnet/minecraft/world/phys/Vec3;Lnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V", at = @At("TAIL"), cancellable = true)
    public void extractRenderState(BannerBlockEntity bannerBlockEntity, BannerRenderState bannerRenderState, float f, Vec3 vec3, ModelFeatureRenderer.CrumblingOverlay crumblingOverlay, CallbackInfo ci) {
        BlockEntityRenderStateExt stateExt = (BlockEntityRenderStateExt)bannerRenderState;
        stateExt.blockEntity(bannerBlockEntity);
    }

    @Inject(method = "submit(Lnet/minecraft/client/renderer/blockentity/state/BannerRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/CameraRenderState;)V", at = @At("HEAD"), cancellable = true)
    public void storeState(BannerRenderState bannerRenderState, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState cameraRenderState, CallbackInfo ci) {
        RENDER_STATE.set(bannerRenderState);
    }

    @Inject(method = "submit(Lnet/minecraft/client/renderer/blockentity/state/BannerRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/CameraRenderState;)V", at = @At("RETURN"))
    public void clearState(CallbackInfo ci) {
        RENDER_STATE.remove();
    }

    @Redirect(method = "submitBanner(Lnet/minecraft/client/resources/model/MaterialSet;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;IIFLnet/minecraft/client/model/object/banner/BannerModel;Lnet/minecraft/client/model/object/banner/BannerFlagModel;FLnet/minecraft/world/item/DyeColor;Lnet/minecraft/world/level/block/entity/BannerPatternLayers;Lnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;I)V", at = @At(value = "INVOKE", target = "net/minecraft/client/renderer/SubmitNodeCollector.submitModel (Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/rendertype/RenderType;IIILnet/minecraft/client/renderer/texture/TextureAtlasSprite;ILnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V"))
    private static <S> void manageSubmitBase(SubmitNodeCollector collector, Model<? super S> model, S state, PoseStack poseStack, RenderType renderType, int light, int overlayCoords, int tint, TextureAtlasSprite textureAtlasSprite, int i4, ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
        if (!ConfigCache.optimizeBanners || !ConfigCache.masterOptimize) {
            collector.submitModel(model, state, poseStack, renderType, light, overlayCoords, tint, textureAtlasSprite, i4, crumblingOverlay);
            return;
        }

        BlockEntityRenderStateExt stateExt = (BlockEntityRenderStateExt)RENDER_STATE.get();

        boolean managed = stateExt != null && OverlayRenderer.manageCrumblingOverlay(stateExt.blockEntity(), poseStack, model, state, light, overlayCoords, tint, crumblingOverlay);
        if (!managed) {
            collector.submitModel(model, state, poseStack, renderType, light, overlayCoords, tint, textureAtlasSprite, i4, crumblingOverlay);
        }
    }

    @Redirect(method = "submitBanner(Lnet/minecraft/client/resources/model/MaterialSet;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;IIFLnet/minecraft/client/model/object/banner/BannerModel;Lnet/minecraft/client/model/object/banner/BannerFlagModel;FLnet/minecraft/world/item/DyeColor;Lnet/minecraft/world/level/block/entity/BannerPatternLayers;Lnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;I)V", at = @At(value = "INVOKE", target = "net/minecraft/client/renderer/blockentity/BannerRenderer.submitPatterns (Lnet/minecraft/client/resources/model/MaterialSet;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;IILnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/client/resources/model/Material;ZLnet/minecraft/world/item/DyeColor;Lnet/minecraft/world/level/block/entity/BannerPatternLayers;ZLnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;I)V"))
    private static <S> void manageSubmitCanvas(MaterialSet materialSet, PoseStack poseStack, SubmitNodeCollector collector, int light, int overlayCoords, Model<S> model, S state, Material material, boolean bl, DyeColor dyeColor, BannerPatternLayers bannerPatternLayers, boolean bl2, ModelFeatureRenderer.CrumblingOverlay crumblingOverlay, int tint) {
        if (!ConfigCache.optimizeBanners || !ConfigCache.masterOptimize) {
            BannerRendererAccessor.invokeSubmitPatterns(materialSet, poseStack, collector, light, overlayCoords, model, state, material, bl, dyeColor, bannerPatternLayers, bl2, crumblingOverlay, tint);
            return;
        }

        BlockEntityRenderStateExt stateExt = (BlockEntityRenderStateExt)RENDER_STATE.get();

        float step = -0.45f;
        float rot = step * ConfigCache.bannerPose;
        float rotClamped = Math.clamp(rot, -4.05f, -0.45f);

        model.root().getChild("flag").xRot = (float)Math.toRadians(rotClamped);

        boolean managed = stateExt != null && OverlayRenderer.manageCrumblingOverlay(stateExt.blockEntity(), poseStack, model, null, light, overlayCoords, tint, crumblingOverlay);
        if (!managed) {
            BannerRendererAccessor.invokeSubmitPatterns(materialSet, poseStack, collector, light, overlayCoords, model, state, material, bl, dyeColor, bannerPatternLayers, bl2, crumblingOverlay, tint);
        }
    }
}
