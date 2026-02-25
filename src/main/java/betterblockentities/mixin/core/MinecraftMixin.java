package betterblockentities.mixin.core;

/* local */
import betterblockentities.client.tasks.ManagerTasks;

/* minecraft */
import net.minecraft.world.level.Level;
import net.minecraft.client.Minecraft;

/* mixin */
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
    @Inject(method = "tick", at = @At("TAIL"))
    private void pollManagerQueue(CallbackInfo ci) {
        Minecraft mc = (Minecraft) (Object) this;
        Level level = mc.level;
        if (level == null || !level.isClientSide()) return;

        ManagerTasks.process();
    }
}
