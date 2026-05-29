package betterblockentities.mixin.render.immediate.blockentity.sign;

/* local */
import betterblockentities.client.BBE;
import betterblockentities.client.gui.config.ConfigCache;

/* minecraft */
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.SignRenderer;
import net.minecraft.world.level.block.state.properties.WoodType;

/* mojang */
import com.mojang.blaze3d.vertex.PoseStack;

/* mixin */
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SignRenderer.class)
public abstract class SignRendererMixin {
    @Inject(method = "renderSign", at = @At("HEAD"), cancellable = true)
    private void skipImmediateSignModel(
            final PoseStack poseStack,
            final MultiBufferSource vertexConsumers,
            final int light,
            final int overlay,
            final WoodType woodType,
            final Model model,
            final CallbackInfo ci
    ) {
        if (!ConfigCache.masterOptimize || !ConfigCache.optimizeSigns) {
            return;
        }

        if (BBE.GlobalScope.limitVanillaSignRendering) {
            ci.cancel();
        }
    }
}
