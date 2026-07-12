package betterblockentities.mixin.render;

import betterblockentities.platform.GlobalScope;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.server.packs.resources.ResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockEntityRenderDispatcher.class)
public class BlockEntityRenderDispatcherMixin {
    @Inject(method = "onResourceManagerReload", at = @At("TAIL"))
    private void bbe$reloadAltDispatcher(ResourceManager resourceManager, CallbackInfo ci) {
        GlobalScope.altRenderDispatcher.onResourceManagerReload(resourceManager);
    }
}
