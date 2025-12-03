package betterblockentities.mixin.minecraft;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.function.Supplier;

@Mixin(OptionsScreen.class)
public interface OptionsScreenAccessor {
    @Invoker("openScreenButton")
    Button callOpenScreenButtonInvoke(Component text, Supplier<Screen> screenSupplier);
}
