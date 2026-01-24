package betterblockentities.mixin.render.immediate.blockentity.chest;

/* local */
import betterblockentities.client.gui.config.ConfigCache;
import betterblockentities.client.model.overrides.ChestModelOverride;

/* minecraft */
import net.minecraft.client.model.geom.ModelLayers;

import net.minecraft.client.model.object.chest.ChestModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.ChestRenderer;
import net.minecraft.client.renderer.blockentity.state.ChestRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;

/* mojang */
import com.mojang.blaze3d.vertex.PoseStack;

/* mixin */
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChestRenderer.class)
public abstract class ChestRendererMixin {
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

    /* replace the original built models with our own that removes the trunk */
    @Inject(method = "submit(Lnet/minecraft/client/renderer/blockentity/state/ChestRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/CameraRenderState;)V", at = @At("HEAD"))
    public void submit(ChestRenderState chestRenderState, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState cameraRenderState, CallbackInfo ci) {
        if (!ConfigCache.optimizeChests || !ConfigCache.masterOptimize) {
            this.singleModel = singleChestOrg;
            this.doubleLeftModel = doubleChestLeftOrg;
            this.doubleRightModel = this.doubleChestRightOrg;
        }
        else {
            this.singleModel = this.BBEsingleChest;
            this.doubleLeftModel = this.BBEdoubleChestLeft;
            this.doubleRightModel = this.BBEdoubleChestRight;
        }
    }
    @Inject(method = "xmasTextures", at = @At("HEAD"), cancellable = true)
    private static void xmasTextures(CallbackInfoReturnable<Boolean> cir) {
        if (ConfigCache.christmasChests && ConfigCache.optimizeChests && ConfigCache.masterOptimize)
            cir.setReturnValue(true);
    }
}
