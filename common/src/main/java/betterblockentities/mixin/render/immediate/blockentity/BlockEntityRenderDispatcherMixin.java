package betterblockentities.mixin.render.immediate.blockentity;

/* local */
import betterblockentities.platform.GlobalScope;

/* minecraft */
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.world.level.block.entity.BlockEntity;

/* mixin */
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

@Mixin(BlockEntityRenderDispatcher.class)
public abstract class BlockEntityRenderDispatcherMixin {
    @Shadow public Camera camera;

    @WrapOperation(
            method = "renderItem",
            at = @At(
                    value = "INVOKE",
                    target = "net/minecraft/client/renderer/blockentity/BlockEntityRenderDispatcher.tryRender (Lnet/minecraft/world/level/block/entity/BlockEntity;Ljava/lang/Runnable;)V",
                    ordinal = 0
            )
    )
    public <E extends BlockEntity> void detectItemInvokedRenderers(BlockEntity blockEntity, Runnable runnable, Operation<Void> original) {
        GlobalScope.isItemInvoked = true;
        original.call(blockEntity, runnable);
        GlobalScope.isItemInvoked = false;
    }
}
