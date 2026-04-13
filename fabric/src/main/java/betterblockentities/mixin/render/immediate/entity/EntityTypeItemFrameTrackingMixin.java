package betterblockentities.mixin.render.immediate.entity;

/* local */
import betterblockentities.client.chunk.pipeline.itemframe.ItemFrameEligibility;

/* minecraft */
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.EntityType;

/* mixin */
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityType.class)
public abstract class EntityTypeItemFrameTrackingMixin {
    @Inject(method = "clientTrackingRange", at = @At("HEAD"), cancellable = true)
    private void extendItemFrameTrackingRange(CallbackInfoReturnable<Integer> cir) {
        EntityType<?> entityType = (EntityType<?>) (Object) this;
        if (entityType != EntityType.ITEM_FRAME && entityType != EntityType.GLOW_ITEM_FRAME) {
            return;
        }

        if (!ItemFrameEligibility.optimizationEnabled()) return;

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.options == null) return;

        cir.setReturnValue(Math.max(10, minecraft.options.getEffectiveRenderDistance()));
    }
}
