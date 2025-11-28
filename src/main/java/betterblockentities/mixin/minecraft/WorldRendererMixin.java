package betterblockentities.mixin.minecraft;

/* local */
import betterblockentities.BetterBlockEntities;

/* minecraft */

/* mixin */
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.culling.Frustum;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public class WorldRendererMixin {
    @Inject(at = @At("HEAD"), method = "cullTerrain", remap = false)
    private void captureFrustum(Camera camera, Frustum frustum, boolean bl, CallbackInfo ci) {
        BetterBlockEntities.curFrustum = frustum;
    }
}
