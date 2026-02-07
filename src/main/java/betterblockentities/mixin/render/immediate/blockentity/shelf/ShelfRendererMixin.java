package betterblockentities.mixin.render.immediate.blockentity.shelf;

/* local */
import betterblockentities.client.gui.config.ConfigCache;

/* minecraft */
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.ShelfRenderer;
import net.minecraft.client.renderer.blockentity.state.ShelfRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;

/* mixin */
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ShelfRenderer.class)
public class ShelfRendererMixin {

    @Inject(method = "submit", at = @At("HEAD"), cancellable = true)
    private void cancelVanillaShelfItems(
            ShelfRenderState shelfRenderState,
            PoseStack poseStack,
            SubmitNodeCollector submitNodeCollector,
            CameraRenderState cameraRenderState,
            CallbackInfo ci
    ) {
        if (ConfigCache.masterOptimize) {
            ci.cancel();
        }
    }
}
