package betterblockentities.mixin.minecraft;

import betterblockentities.gui.ConfigScreen;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Environment(EnvType.CLIENT)
@Mixin(OptionsScreen.class)
public abstract class OptionsScreenMixin {
    @Inject(
            method = "init",
            at = @At(
                    value = "INVOKE_ASSIGN",
                    target = "Lnet/minecraft/client/gui/layouts/GridLayout;createRowHelper(I)Lnet/minecraft/client/gui/layouts/GridLayout$RowHelper;"
            ),
            locals = LocalCapture.CAPTURE_FAILHARD
    )
    private void addMyModButton(CallbackInfo ci, LinearLayout linearLayout, LinearLayout linearLayout2, GridLayout gridLayout, GridLayout.RowHelper rowHelper) {
        OptionsScreenAccessor acc = (OptionsScreenAccessor)(Object)this;
        Button btn = acc.callOpenScreenButtonInvoke(
                Component.literal("Better Block Entities"),
                () -> new ConfigScreen((Screen)(Object)this)
        );
        rowHelper.addChild(btn);
    }
}
