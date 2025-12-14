package betterblockentities.mixin.minecraft;

/* local */
import betterblockentities.util.BlockEntityTracker;

/* minecraft */
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

/* mixin */
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

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
}
