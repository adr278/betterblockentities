package betterblockentities.gui;

/* minecraft */
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.network.chat.Component;

/*
    TODO: clean this shit up lol
*/

public class ConfigScreen extends OptionsSubScreen {
    private final ConfigHolder originalConfig;

    private OptionInstance<Boolean> masterToggle;
    private OptionInstance<Boolean>
            chestOpt,
            signOpt,
            shulkerOpt,
            bedOpt,
            bellOpt,
            potOpt,
            chestAnimOpt,
            chestChristmasOpt,
            signTextOpt,
            shulkerAnimOpt,
            bellAnimOpt,
            potAnimOpt;
    private OptionInstance<Integer>
            updateType,
            smoothness;
    private OptionInstance<Integer> signDistance;

    public ConfigScreen(Screen parent) {
        super(parent, Minecraft.getInstance().options, Component.translatable("Better Block Entities"));
        this.originalConfig = ConfigManager.CONFIG.copy();
    }

    @Override
    protected void addOptions() {
        if (this.list == null) return;

        masterToggle = masterToggle();
        chestOpt = optimizeChests();
        signOpt = optimizeSigns();
        shulkerOpt = optimizeShulkers();
        bedOpt = optimizeBeds();
        bellOpt = optimizeBells();
        potOpt = optimizeDecoratedPots();
        updateType = updateType();
        smoothness = extraRenderPasses();
        signDistance = signTextRenderDistance();

        chestAnimOpt = chestsAnimations();
        chestChristmasOpt = chestsChristmas();
        signTextOpt = renderSignText();
        shulkerAnimOpt = shulkerAnimations();
        bellAnimOpt = bellAnimations();
        potAnimOpt = potAnimations();

        this.list.addBig(masterToggle);
        this.list.addSmall(
                chestOpt, chestAnimOpt
        );
        this.list.addSmall(chestChristmasOpt);
        this.list.addSmall(
                signOpt, signTextOpt,
                shulkerOpt, shulkerAnimOpt,
                bellOpt, bellAnimOpt,
                potOpt, potAnimOpt,
                bedOpt
        );
        this.list.addBig(updateType);
        this.list.addBig(smoothness);
        this.list.addBig(signDistance);
        updateDependentOptions(masterToggle.get());
    }

    private OptionInstance<Boolean> masterToggle() {
        return new OptionInstance<>(
                "Enable Optimizations",
                value -> Tooltip.create(Component.literal("§7Turns the entire optimization system on or off.")),
                (text, value) -> value ? Component.literal("§aON") : Component.literal("§cOFF"),
                OptionInstance.BOOLEAN_VALUES,
                ConfigManager.CONFIG.master_optimize,
                value -> {
                    ConfigManager.CONFIG.master_optimize = value;
                    updateDependentOptions(value);
                }
        );
    }

    private OptionInstance<Boolean> optimizeChests() {
        return new OptionInstance<>(
                "Optimize Chests",
                value -> Tooltip.create(Component.literal("§7Turns off all Chest optimizations, overrides the option: §l§nChest Animations§r")),
                (text, value) -> value ? Component.literal("§aON") : Component.literal("§cOFF"),
                OptionInstance.BOOLEAN_VALUES,
                ConfigManager.CONFIG.optimize_chests,
                v -> {
                    ConfigManager.CONFIG.optimize_chests = v;
                    setOptionActive(chestAnimOpt, v && masterToggle.get());
                }
        );
    }

    private OptionInstance<Boolean> chestsAnimations() {
        return booleanOption(
                "Chest Animations",
                ConfigManager.CONFIG.chest_animations,
                v -> ConfigManager.CONFIG.chest_animations = v
        );
    }

    private OptionInstance<Boolean> chestsChristmas() {
        return booleanOption(
                "Christmas Chests",
                ConfigManager.CONFIG.chest_christmas,
                v -> ConfigManager.CONFIG.chest_christmas = v
        );
    }

    private OptionInstance<Boolean> optimizeSigns() {
        return new OptionInstance<>(
                "Optimize Signs",
                value -> Tooltip.create(Component.literal("§7Turns off all Sign optimizations, overrides the option: §l§nSign Text§r")),
                (text, value) -> value ? Component.literal("§aON") : Component.literal("§cOFF"),
                OptionInstance.BOOLEAN_VALUES,
                ConfigManager.CONFIG.optimize_signs,
                v -> {
                    ConfigManager.CONFIG.optimize_signs = v;
                    setOptionActive(signTextOpt, v && masterToggle.get());
                }
        );
    }

    private OptionInstance<Boolean> renderSignText() {
        return booleanOption(
                "Sign Text",
                ConfigManager.CONFIG.render_sign_text,
                v -> ConfigManager.CONFIG.render_sign_text = v
        );
    }

    private OptionInstance<Boolean> optimizeShulkers() {
        return new OptionInstance<>(
                "Optimize Shulkers",
                value -> Tooltip.create(Component.literal("§7Turns off all ShulkerBox optimizations, overrides the option: §l§nShulker Animations§r")),
                (text, value) -> value ? Component.literal("§aON") : Component.literal("§cOFF"),
                OptionInstance.BOOLEAN_VALUES,
                ConfigManager.CONFIG.optimize_shulkers,
                v -> {
                    ConfigManager.CONFIG.optimize_shulkers = v;
                    setOptionActive(shulkerAnimOpt, v && masterToggle.get());
                }
        );
    }

    private OptionInstance<Boolean> shulkerAnimations() {
        return booleanOption(
                "Shulker Animations",
                ConfigManager.CONFIG.shulker_animations,
                v -> ConfigManager.CONFIG.shulker_animations = v
        );
    }

    private OptionInstance<Boolean> optimizeBeds() {
        return new OptionInstance<>(
                "Optimize Beds",
                value -> Tooltip.create(Component.literal("§7Turns off all Bed optimizations")),
                (text, value) -> value ? Component.literal("§aON") : Component.literal("§cOFF"),
                OptionInstance.BOOLEAN_VALUES,
                ConfigManager.CONFIG.optimize_beds,
                v -> {
                    ConfigManager.CONFIG.optimize_beds = v;
                    setOptionActive(masterToggle, v && masterToggle.get());
                }
        );
    }

    private OptionInstance<Boolean> optimizeBells() {
        return new OptionInstance<>(
                "Optimize Bells",
                value -> Tooltip.create(Component.literal("§7Turns off all Bell optimizations, overrides the option: §l§nBell Animations§r")),
                (text, value) -> value ? Component.literal("§aON") : Component.literal("§cOFF"),
                OptionInstance.BOOLEAN_VALUES,
                ConfigManager.CONFIG.optimize_bells,
                v -> {
                    ConfigManager.CONFIG.optimize_bells = v;
                    setOptionActive(bellAnimOpt, v && masterToggle.get());
                }
        );
    }

    private OptionInstance<Boolean> bellAnimations() {
        return booleanOption(
                "Bell Animations",
                ConfigManager.CONFIG.bell_animations,
                v -> ConfigManager.CONFIG.bell_animations = v
        );
    }

    private OptionInstance<Boolean> optimizeDecoratedPots() {
        return new OptionInstance<>(
                "Optimize Decorated Pots",
                value -> Tooltip.create(Component.literal("§7Turns off all Decorated Pot optimizations, overrides the option: §l§nDecorated Pot Animations§r")),
                (text, value) -> value ? Component.literal("§aON") : Component.literal("§cOFF"),
                OptionInstance.BOOLEAN_VALUES,
                ConfigManager.CONFIG.optimize_decoratedpots,
                v -> {
                    ConfigManager.CONFIG.optimize_decoratedpots = v;
                    setOptionActive(potAnimOpt, v && masterToggle.get());
                }
        );
    }

    private OptionInstance<Boolean> potAnimations() {
        return booleanOption(
                "Decorated Pot Animations",
                ConfigManager.CONFIG.pot_animations,
                v -> ConfigManager.CONFIG.pot_animations = v
        );
    }

    private OptionInstance<Integer> signTextRenderDistance() {
        return new OptionInstance<>(
                "Sign Text Render Distance",
                value -> Tooltip.create(Component.literal("§7The amount of blocks the sign text will stop rendering at")),
                (text, value) -> Component.literal(text.getString() + ": " + value),
                new OptionInstance.IntRange(0, 64),
                ConfigManager.CONFIG.sign_text_render_distance,
                v -> ConfigManager.CONFIG.sign_text_render_distance = v
        );
    }

    private OptionInstance<Integer> updateType() {
        return new OptionInstance<>(
                "Update Type",
                value -> Tooltip.create(Component.literal("§7Type of update scheduler being used. §l§nSmart§r §7updates only when the BE is not in line of sight or out of FOV. §l§nFast§r §7updates immediately")),
                (text, value) -> switch (value) {
                    case 0 -> Component.literal("Smart");
                    case 1 -> Component.literal("Fast");
                    default -> Component.literal("Fast");
                },
                new OptionInstance.ClampingLazyMaxIntRange(0, () -> 1, 1),
                ConfigManager.CONFIG.updateType,
                value -> ConfigManager.CONFIG.updateType = value
        );
    }

    private OptionInstance<Integer> extraRenderPasses() {
        return new OptionInstance<>(
                "Extra Render Passes",
                value -> Tooltip.create(Component.literal("§7The amount of extra render passes each optimized block entity should be rendered for after it stops animating, can help smooth out visual bugs")),
                (text, value) -> Component.literal(text.getString() + ": " + value),
                new OptionInstance.IntRange(0, 50),
                ConfigManager.CONFIG.smoothness_slider,
                v -> ConfigManager.CONFIG.smoothness_slider = v
        );
    }

    private OptionInstance<Boolean> booleanOption(String key, boolean initial, java.util.function.Consumer<Boolean> onChange) {
        return new OptionInstance<>(
                key,
                OptionInstance.noTooltip(),
                (text, value) -> value ? Component.literal("§aON") : Component.literal("§cOFF"),
                OptionInstance.BOOLEAN_VALUES,
                initial,
                onChange
        );
    }

    private void updateDependentOptions(boolean enabled) {
        setOptionActive(chestOpt, enabled);
        setOptionActive(signOpt, enabled);
        setOptionActive(shulkerOpt, enabled);
        setOptionActive(bedOpt, enabled);
        setOptionActive(bellOpt, enabled);
        setOptionActive(potOpt, enabled);

        setOptionActive(chestAnimOpt, enabled && chestOpt.get());
        setOptionActive(chestChristmasOpt, enabled && chestOpt.get());
        setOptionActive(signTextOpt, enabled && signOpt.get());
        setOptionActive(shulkerAnimOpt, enabled && shulkerOpt.get());
        setOptionActive(bellAnimOpt, enabled && bellOpt.get());
        setOptionActive(potAnimOpt, enabled && potOpt.get());

        setOptionActive(smoothness, enabled);
        setOptionActive(updateType, enabled);
        setOptionActive(signDistance, enabled);
    }

    private void setOptionActive(OptionInstance<?> option, boolean active) {
        if (this.list == null) return;
        AbstractWidget widget = this.list.findOption(option);
        if (widget != null) widget.active = active;
    }

    @Override
    public void removed() {
        if (!ConfigManager.CONFIG.equals(originalConfig)) {
            ConfigManager.save();
            ConfigManager.refreshSupportedTypes();
            Minecraft.getInstance().reloadResourcePacks();
        }
    }
}
