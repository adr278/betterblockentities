package betterblockentities.mixin.render.immediate.blockentity.chest;

/* local */
import betterblockentities.client.gui.config.ConfigCache;
import betterblockentities.client.model.overrides.ChestModelOverride;
import betterblockentities.client.render.immediate.OverlayRenderer;
import betterblockentities.client.render.immediate.blockentity.BlockEntityRenderStateExt;

/* minecraft */
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.object.chest.ChestModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.ChestRenderer;
import net.minecraft.client.renderer.blockentity.state.ChestRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.LidBlockEntity;
import net.minecraft.world.phys.Vec3;

/* mojang */
import com.mojang.blaze3d.vertex.PoseStack;

/* mixin */
import com.llamalad7.mixinextras.sugar.Local;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChestRenderer.class)
public abstract class ChestRendererMixin <T extends BlockEntity & LidBlockEntity> {
    @Shadow @Mutable private ChestModel singleModel;
    @Shadow @Mutable private ChestModel doubleLeftModel;
    @Shadow @Mutable private ChestModel doubleRightModel;

    @Unique private ChestModel BBEsingleChest;
    @Unique private ChestModel BBEdoubleChestLeft;
    @Unique private ChestModel BBEdoubleChestRight;

    @Unique private ChestModel singleChestOrg;
    @Unique private ChestModel doubleChestLeftOrg;
    @Unique private ChestModel doubleChestRightOrg;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void cacheAndInitModels(BlockEntityRendererProvider.Context context, CallbackInfo ci) {
        this.singleChestOrg = new ChestModel(context.bakeLayer(ModelLayers.CHEST));
        this.doubleChestLeftOrg = new ChestModel(context.bakeLayer(ModelLayers.DOUBLE_CHEST_LEFT));
        this.doubleChestRightOrg = new ChestModel(context.bakeLayer(ModelLayers.DOUBLE_CHEST_RIGHT));

        this.BBEsingleChest = new ChestModelOverride(context.bakeLayer(ModelLayers.CHEST));
        this.BBEdoubleChestLeft = new ChestModelOverride(context.bakeLayer(ModelLayers.DOUBLE_CHEST_LEFT));
        this.BBEdoubleChestRight = new ChestModelOverride(context.bakeLayer(ModelLayers.DOUBLE_CHEST_RIGHT));
    }

    @Inject(method = "extractRenderState(Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/client/renderer/blockentity/state/ChestRenderState;FLnet/minecraft/world/phys/Vec3;Lnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V", at = @At("TAIL"), cancellable = true)
    public void extractRenderState(T blockEntity, ChestRenderState chestRenderState, float f, Vec3 vec3, ModelFeatureRenderer.CrumblingOverlay crumblingOverlay, CallbackInfo ci) {
        BlockEntityRenderStateExt stateExt = (BlockEntityRenderStateExt)chestRenderState;
        stateExt.blockEntity(blockEntity);
    }

    @Redirect(method = "submit(Lnet/minecraft/client/renderer/blockentity/state/ChestRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/CameraRenderState;)V", at = @At(value = "INVOKE", target = "net/minecraft/client/renderer/SubmitNodeCollector.submitModel (Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/rendertype/RenderType;IIILnet/minecraft/client/renderer/texture/TextureAtlasSprite;ILnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V"))
    public <S> void manageSubmit(SubmitNodeCollector collector, Model<? super S> model, S state, PoseStack poseStack, RenderType renderType, int light, int overlayCoords, int tint, TextureAtlasSprite textureAtlasSprite, int i4, ModelFeatureRenderer.CrumblingOverlay crumblingOverlay, @Local(index = 1)ChestRenderState chestRenderState) {
        this.singleModel = singleChestOrg;
        this.doubleLeftModel = doubleChestLeftOrg;
        this.doubleRightModel = this.doubleChestRightOrg;

        if (!ConfigCache.optimizeChests || !ConfigCache.masterOptimize) {
            collector.submitModel(model, state, poseStack, renderType, light, overlayCoords, tint, textureAtlasSprite, i4, crumblingOverlay);
            return;
        }

        BlockEntityRenderStateExt stateExt = (BlockEntityRenderStateExt)chestRenderState;

        boolean managed = OverlayRenderer.manageCrumblingOverlay(stateExt.blockEntity(), poseStack, model, state, light, overlayCoords, tint, crumblingOverlay);
        if (!managed) {
            this.singleModel = this.BBEsingleChest;
            this.doubleLeftModel = this.BBEdoubleChestLeft;
            this.doubleRightModel = this.BBEdoubleChestRight;

            collector.submitModel(model, state, poseStack, renderType, light, overlayCoords, tint, textureAtlasSprite, i4, crumblingOverlay);
        }
    }

    @Inject(method = "xmasTextures", at = @At("HEAD"), cancellable = true)
    private static void xmasTextures(CallbackInfoReturnable<Boolean> cir) {
        if (ConfigCache.christmasChests && ConfigCache.optimizeChests && ConfigCache.masterOptimize)
            cir.setReturnValue(true);
    }
}
