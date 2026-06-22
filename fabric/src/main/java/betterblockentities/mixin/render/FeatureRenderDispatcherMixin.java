package betterblockentities.mixin.render;

/* local */
import betterblockentities.client.BBE;

/* minecraft */
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.FeatureFrameContext;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;

/* mixin */
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

/* java/misc */
import java.util.Map;

@Mixin(FeatureRenderDispatcher.class)
public class FeatureRenderDispatcherMixin {
    @WrapOperation(
            method = "prepareFrameWithContext",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/feature/FeatureRenderDispatcher$PreparedFrame;begin(Lnet/minecraft/client/renderer/feature/FeatureFrameContext;Lnet/minecraft/client/renderer/SubmitNodeStorage;)Lnet/minecraft/client/renderer/feature/FeatureRenderDispatcher$PreparedFrame;"
            )
    )
    private FeatureRenderDispatcher.PreparedFrame drainOverlayStorage(
            FeatureRenderDispatcher.PreparedFrame instance,
            FeatureFrameContext context,
            SubmitNodeStorage submitNodeStorage,
            Operation<FeatureRenderDispatcher.PreparedFrame> original
    ) {
        this.mergeOverlayStorage(submitNodeStorage, BBE.GlobalScope.overlayNodeStorage);

        original.call(instance, context, submitNodeStorage);
        return instance;
    }

    @Unique
    private void mergeOverlayStorage(SubmitNodeStorage target, SubmitNodeStorage overlay) {
        Map<Integer, SubmitNodeCollection> targetOrders = target.getSubmitsPerOrder();
        Map<Integer, SubmitNodeCollection> overlayOrders = overlay.getSubmitsPerOrder();

        overlayOrders.forEach((order, collection) -> {
            int candidateOrder = order + 1;

            while (targetOrders.putIfAbsent(candidateOrder, collection) != null) {
                candidateOrder++;
            }
        });
        overlayOrders.clear();
    }
}