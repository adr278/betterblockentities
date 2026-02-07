package betterblockentities.mixin.render.immediate.blockentity.shelf;

/* local */
import betterblockentities.client.gui.config.ConfigCache;

/* minecraft */
import net.minecraft.client.renderer.blockentity.ShelfRenderer;
import net.minecraft.client.renderer.blockentity.state.ShelfRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.world.level.block.entity.ShelfBlockEntity;
import net.minecraft.world.phys.Vec3;

/* mixin */
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.Arrays;

@Mixin(ShelfRenderer.class)
@SuppressWarnings("unused") public abstract class ShelfRendererMixin {

    @Inject(method = "extractRenderState", at = @At("HEAD"), cancellable = true)
    private void cancelShelfItemExtraction(
            ShelfBlockEntity shelf,
            ShelfRenderState state,
            float partialTick,
            Vec3 cameraPos,
            ModelFeatureRenderer.CrumblingOverlay crumblingOverlay,
            CallbackInfo ci
    ) {
        if (!ConfigCache.masterOptimize || !ConfigCache.optimizeShelf) return;

        ci.cancel();
    }
}
