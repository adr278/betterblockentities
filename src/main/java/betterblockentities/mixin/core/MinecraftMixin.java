package betterblockentities.mixin.core;

/* local */
import betterblockentities.client.model.BBEGeometryRegistry;
import betterblockentities.client.render.immediate.blockentity.BlockEntityTracker;

/* minecraft */
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

/* mixin */
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.CompletableFuture;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
    @Inject(method = "tick", at = @At("TAIL"))
    private void onClientTick(CallbackInfo ci) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) return;

        BlockEntityTracker.animMap.removeIf(
                pos -> client.level.getBlockEntity(BlockPos.of(pos)) == null
        );
    }

    /* clear registry cache before we start processing so we don't have old geometry hanging around */
    @Inject(method = "reloadResourcePacks()Ljava/util/concurrent/CompletableFuture;", at = @At("HEAD"))
    public void reloadResourcePacks(CallbackInfoReturnable<CompletableFuture<Void>> cir) {
        BBEGeometryRegistry.clearCache();
    }
}
