package betterblockentities.mixin.minecraft;

import betterblockentities.gui.ConfigManager;
import net.minecraft.util.SpecialDates;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.time.MonthDay;
import java.time.ZonedDateTime;
import java.util.List;

@Mixin(SpecialDates.class)
public abstract class SpecialDatesMixin {
    @Shadow
    @Final
    public static List<MonthDay> CHRISTMAS_RANGE;

    @Inject(method = "isExtendedChristmas", at = @At("HEAD"), cancellable = true)
    private static void isExtendedChristmas(CallbackInfoReturnable<Boolean> cir) {
        boolean isChristmas = CHRISTMAS_RANGE.contains(MonthDay.from(ZonedDateTime.now()));
        boolean forceChristmasTex = ConfigManager.CONFIG.chest_christmas;

        if (ConfigManager.CONFIG.master_optimize && ConfigManager.CONFIG.optimize_chests)
            cir.setReturnValue(forceChristmasTex);
        else
            cir.setReturnValue(isChristmas);
    }
}
