package betterblockentities.gui;

/* sodium */
import net.caffeinemc.mods.sodium.api.config.ConfigEntryPoint;
import net.caffeinemc.mods.sodium.api.config.structure.ConfigBuilder;

/* minecraft */
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public class SodiumUIImpl implements ConfigEntryPoint {
    @Override
    public void registerConfigLate(ConfigBuilder builder) {
        builder.registerOwnModOptions()
                .setIcon(Identifier.parse("betterblockentities:icon.png"))
                .addPage(builder.createExternalPage()
                        .setName(Component.literal("BBE Config"))
                        .setScreenConsumer(parent -> {
                            Minecraft.getInstance().setScreen(new ConfigScreen(parent));
                        })
                );
    }
}
