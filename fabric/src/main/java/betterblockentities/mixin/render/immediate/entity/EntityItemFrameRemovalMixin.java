package betterblockentities.mixin.render.immediate.entity;

/* local */
import betterblockentities.client.chunk.pipeline.itemframe.ItemFrameEligibility;
import betterblockentities.client.render.immediate.entity.ItemFrameRuntimeHelper;

/* minecraft */
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.entity.decoration.ItemFrame;

/* mixin */
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityItemFrameRemovalMixin {
    @Inject(method = "setRemoved", at = @At("HEAD"))
    private void trackRemovedItemFrames(RemovalReason reason, CallbackInfo ci) {
        if (!ItemFrameEligibility.optimizationEnabled()) return;

        Entity self = self();
        if (!(self instanceof ItemFrame frame)) return;
        if (frame.isRemoved()) return;

        ItemFrameRuntimeHelper.onRemoved(frame);
    }

    @Unique private Entity self() { return (Entity) (Object) this; }
}
