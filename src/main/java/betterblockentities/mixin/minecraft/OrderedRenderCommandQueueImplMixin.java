package betterblockentities.mixin.minecraft;

/* local */
import betterblockentities.gui.ConfigManager;
import betterblockentities.util.BlockEntityManager;

/* minecraft */
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.SubmitNodeStorage;

/*mojang */
import com.mojang.blaze3d.vertex.PoseStack;

/* mixin */
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SubmitNodeCollection.class)
public class OrderedRenderCommandQueueImplMixin {
    @Shadow @Final private SubmitNodeStorage submitNodeStorage;

    @Inject(method = "submitBlock", at = @At("HEAD"), cancellable = true)
    public void submitBlock(PoseStack poseStack, net.minecraft.world.level.block.state.BlockState state, int i, int j, int k, CallbackInfo ci) {
        if (BlockEntityManager.isSupportedBlock(state.getBlock()) && ConfigManager.CONFIG.master_optimize) {
            ci.cancel();
            Minecraft.getInstance()
                    .getModelManager()
                    .specialBlockModelRenderer()
                    .renderByBlock(state.getBlock(), net.minecraft.world.item.ItemDisplayContext.NONE, poseStack, this.submitNodeStorage, i, j, k);
        }
    }
}
