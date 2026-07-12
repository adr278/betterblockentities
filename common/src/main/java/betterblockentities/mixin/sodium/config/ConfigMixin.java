package betterblockentities.mixin.sodium.config;

/* local */
import betterblockentities.client.gui.config.BBEConfig;

/* sodium */
import net.caffeinemc.mods.sodium.client.config.structure.Config;

/* mixin */
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * I couldn't find a builder option for "running some code after apply is pressed" so this will have to do
 */
@Mixin(Config.class)
public class ConfigMixin {
    @Inject(method = "applyAllOptions", at = @At("RETURN"))
    public void bbe$updateConfigStorage(CallbackInfo ci) {
        BBEConfig.updateConfigCache();
    }
}
