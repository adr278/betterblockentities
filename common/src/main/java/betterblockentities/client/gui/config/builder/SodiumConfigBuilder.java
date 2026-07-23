package betterblockentities.client.gui.config.builder;

/* local */
import betterblockentities.client.gui.option.EnumTypes;
import betterblockentities.client.gui.storage.SodiumConfigStorage;
import betterblockentities.platform.GlobalScope;

/* sodium */
import net.caffeinemc.mods.sodium.api.config.ConfigEntryPoint;
import net.caffeinemc.mods.sodium.api.config.StorageEventHandler;
import net.caffeinemc.mods.sodium.api.config.option.OptionFlag;
import net.caffeinemc.mods.sodium.api.config.option.OptionImpact;
import net.caffeinemc.mods.sodium.api.config.option.Range;
import net.caffeinemc.mods.sodium.api.config.structure.BooleanOptionBuilder;
import net.caffeinemc.mods.sodium.api.config.structure.ConfigBuilder;
import net.caffeinemc.mods.sodium.api.config.structure.OptionPageBuilder;

/* minecraft */
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class SodiumConfigBuilder implements ConfigEntryPoint {
    private static final ResourceLocation MASTER = ResourceLocation.parse("bbe:master");

    private final SodiumConfigStorage bbeSodiumConfigStorage = new SodiumConfigStorage();
    private final StorageEventHandler saveMainConfigStorageObject = bbeSodiumConfigStorage::save;

    private void registerGeneralPage(ConfigBuilder builder, OptionPageBuilder page) {
        page.addOptionGroup(builder.createOptionGroup()
                .addOption(
                        builder.createBooleanOption(MASTER)
                                .setName(Component.translatable("bbe.config.storage.main.optimize.master"))
                                .setTooltip(Component.translatable("bbe.config.storage.main.optimize.master.tooltip"))
                                .setDefaultValue(true)
                                .setImpact(OptionImpact.HIGH)
                                .setBinding(
                                        value -> GlobalScope.CONFIG.MAIN.setOption("optimize.master", value),
                                        () -> (boolean) GlobalScope.CONFIG.MAIN.getOption("optimize.master").getValue()
                                )
                                .setFlags(OptionFlag.REQUIRES_ASSET_RELOAD)
                                .setStorageHandler(this.saveMainConfigStorageObject)
                ));

        page.addOptionGroup(builder.createOptionGroup()
                .addOption(
                        builder.createEnumOption(ResourceLocation.parse("bbe:misc.shademode"), EnumTypes.ShadeMode.class)
                                .setName(Component.translatable("bbe.config.storage.main.misc.shademode"))
                                .setTooltip(Component.translatable("bbe.config.storage.main.misc.shademode.tooltip"))
                                .setDefaultValue(EnumTypes.ShadeMode.SODIUM)
                                .setImpact(OptionImpact.VARIES)
                                .setBinding(
                                        value -> GlobalScope.CONFIG.MAIN.setOption("misc.shademode", EnumTypes.ShadeMode.map(value)),
                                        () -> EnumTypes.ShadeMode.map((int) GlobalScope.CONFIG.MAIN.getOption("misc.shademode").getValue())
                                )
                                .setElementNameProvider(e -> new Component[]{
                                        Component.translatable("bbe.config.storage.main.misc.shademode.type.sodium"),
                                        Component.translatable("bbe.config.storage.main.misc.shademode.type.vanilla")
                                }[e.ordinal()])
                                .setEnabledProvider(c -> c.readBooleanOption(MASTER), MASTER)
                                .setFlags(OptionFlag.REQUIRES_ASSET_RELOAD)
                                .setStorageHandler(this.saveMainConfigStorageObject)
                )
                .addOption(
                        builder.createEnumOption(ResourceLocation.parse("bbe:misc.update_scheduler"), EnumTypes.UpdateSchedulerType.class)
                                .setName(Component.translatable("bbe.config.storage.main.misc.update_scheduler"))
                                .setTooltip(Component.translatable("bbe.config.storage.main.misc.update_scheduler.tooltip"))
                                .setDefaultValue(EnumTypes.UpdateSchedulerType.SMART)
                                .setImpact(OptionImpact.VARIES)
                                .setBinding(
                                        value -> GlobalScope.CONFIG.MAIN.setOption("misc.update_scheduler", EnumTypes.UpdateSchedulerType.map(value)),
                                        () -> EnumTypes.UpdateSchedulerType.map((int) GlobalScope.CONFIG.MAIN.getOption("misc.update_scheduler").getValue())
                                )
                                .setElementNameProvider(e -> new Component[]{
                                        Component.translatable("bbe.config.storage.main.misc.update_scheduler.type.fast"),
                                        Component.translatable("bbe.config.storage.main.misc.update_scheduler.type.smart")
                                }[e.ordinal()])
                                .setEnabledProvider(c -> c.readBooleanOption(MASTER), MASTER)
                                .setFlags(OptionFlag.REQUIRES_ASSET_RELOAD)
                                .setStorageHandler(this.saveMainConfigStorageObject)
                ));
    }

    private void registerOptimizationPage(ConfigBuilder builder, OptionPageBuilder page) {
        ResourceLocation sign = ResourceLocation.parse("bbe:optimize.sign");
        ResourceLocation signText = ResourceLocation.parse("bbe:misc.sign_text");

        page.addOptionGroup(builder.createOptionGroup()
                .addOption(
                        builder.createBooleanOption(sign)
                                .setName(Component.translatable("bbe.config.storage.main.optimize.sign"))
                                .setTooltip(Component.translatable("bbe.config.storage.main.optimize.sign.tooltip"))
                                .setDefaultValue(true)
                                .setImpact(OptionImpact.HIGH)
                                .setBinding(
                                        value -> GlobalScope.CONFIG.MAIN.setOption("optimize.sign", value),
                                        () -> (boolean) GlobalScope.CONFIG.MAIN.getOption("optimize.sign").getValue()
                                )
                                .setEnabledProvider(c -> c.readBooleanOption(MASTER), MASTER)
                                .setFlags(OptionFlag.REQUIRES_ASSET_RELOAD)
                                .setStorageHandler(this.saveMainConfigStorageObject)
                )
                .addOption(
                        builder.createBooleanOption(signText)
                                .setName(Component.translatable("bbe.config.storage.main.misc.sign_text"))
                                .setTooltip(Component.translatable("bbe.config.storage.main.misc.sign_text.tooltip"))
                                .setDefaultValue(true)
                                .setImpact(OptionImpact.MEDIUM)
                                .setBinding(
                                        value -> GlobalScope.CONFIG.MAIN.setOption("misc.sign_text", value),
                                        () -> (boolean) GlobalScope.CONFIG.MAIN.getOption("misc.sign_text").getValue()
                                )
                                .setEnabledProvider(c -> c.readBooleanOption(MASTER) && c.readBooleanOption(sign), MASTER, sign)
                                .setFlags(OptionFlag.REQUIRES_ASSET_RELOAD)
                                .setStorageHandler(this.saveMainConfigStorageObject)
                )
                .addOption(
                        builder.createIntegerOption(ResourceLocation.parse("bbe:misc.sign_text_distance"))
                                .setName(Component.translatable("bbe.config.storage.main.misc.sign_text_distance"))
                                .setTooltip(Component.translatable("bbe.config.storage.main.misc.sign_text_distance.tooltip"))
                                .setDefaultValue(16)
                                .setImpact(OptionImpact.MEDIUM)
                                .setBinding(
                                        value -> GlobalScope.CONFIG.MAIN.setOption("misc.sign_text_distance", value),
                                        () -> (int) GlobalScope.CONFIG.MAIN.getOption("misc.sign_text_distance").getValue()
                                )
                                .setRange(new Range(1, 64, 1))
                                .setValueFormatter(value -> Component.literal(value + " blocks"))
                                .setEnabledProvider(c -> c.readBooleanOption(MASTER) && c.readBooleanOption(sign)
                                        && c.readBooleanOption(signText), MASTER, sign, signText)
                                .setFlags(OptionFlag.REQUIRES_ASSET_RELOAD)
                                .setStorageHandler(this.saveMainConfigStorageObject)
                )
                .addOption(
                        builder.createBooleanOption(ResourceLocation.parse("bbe:misc.sign_text_culling"))
                                .setName(Component.translatable("bbe.config.storage.main.misc.sign_text_culling"))
                                .setTooltip(Component.translatable("bbe.config.storage.main.misc.sign_text_culling.tooltip"))
                                .setDefaultValue(true)
                                .setImpact(OptionImpact.MEDIUM)
                                .setBinding(
                                        value -> GlobalScope.CONFIG.MAIN.setOption("misc.sign_text_culling", value),
                                        () -> (boolean) GlobalScope.CONFIG.MAIN.getOption("misc.sign_text_culling").getValue()
                                )
                                .setEnabledProvider(c -> c.readBooleanOption(MASTER) && c.readBooleanOption(sign)
                                        && c.readBooleanOption(signText), MASTER, sign, signText)
                                .setFlags(OptionFlag.REQUIRES_ASSET_RELOAD)
                                .setStorageHandler(this.saveMainConfigStorageObject)
                ));

        page.addOptionGroup(builder.createOptionGroup()
                .addOption(optimizationToggle(builder, "chest", "chest"))
                .addOption(optimizationToggle(builder, "shulker", "shulker"))
                .addOption(optimizationToggle(builder, "decoratedpot", "decoratedpot"))
                .addOption(optimizationToggle(builder, "banner", "banner"))
                .addOption(optimizationToggle(builder, "bell", "bell"))
                .addOption(optimizationToggle(builder, "bed", "bed"))
                .addOption(optimizationToggle(builder, "campfire", "campfire"))
                .addOption(optimizationToggle(builder, "lectern", "lectern"))
        );
    }

    private BooleanOptionBuilder optimizationToggle(
            ConfigBuilder builder,
            String id,
            String storageKey
    ) {
        return builder.createBooleanOption(ResourceLocation.parse("bbe:optimize." + id))
                .setName(Component.translatable("bbe.config.storage.main.optimize." + id))
                .setTooltip(Component.translatable("bbe.config.storage.main.optimize." + id + ".tooltip"))
                .setDefaultValue(true)
                .setImpact(OptionImpact.HIGH)
                .setBinding(
                        value -> GlobalScope.CONFIG.MAIN.setOption("optimize." + storageKey, value),
                        () -> (boolean) GlobalScope.CONFIG.MAIN.getOption("optimize." + storageKey).getValue()
                )
                .setEnabledProvider(c -> c.readBooleanOption(MASTER), MASTER)
                .setFlags(OptionFlag.REQUIRES_ASSET_RELOAD)
                .setStorageHandler(this.saveMainConfigStorageObject);
    }

    private void registerAnimationPage(ConfigBuilder builder, OptionPageBuilder page) {
        page.addOptionGroup(builder.createOptionGroup()
                .addOption(animationToggle(builder, "chest", "chest"))
                .addOption(animationToggle(builder, "shulker", "shulker"))
                .addOption(animationToggle(builder, "decoratedpot", "decoratedpot"))
                .addOption(animationToggle(builder, "bell", "bell"))
        );
    }

    private BooleanOptionBuilder animationToggle(
            ConfigBuilder builder,
            String id,
            String storageKey
    ) {
        ResourceLocation optimization = ResourceLocation.parse("bbe:optimize." + id);
        return builder.createBooleanOption(ResourceLocation.parse("bbe:animation." + id))
                .setName(Component.translatable("bbe.config.storage.main.animation." + id))
                .setTooltip(Component.translatable("bbe.config.storage.main.animation." + id + ".tooltip"))
                .setDefaultValue(true)
                .setImpact(OptionImpact.LOW)
                .setBinding(
                        value -> GlobalScope.CONFIG.MAIN.setOption("animation." + storageKey, value),
                        () -> (boolean) GlobalScope.CONFIG.MAIN.getOption("animation." + storageKey).getValue()
                )
                .setEnabledProvider(c -> c.readBooleanOption(MASTER) && c.readBooleanOption(optimization), MASTER, optimization)
                .setFlags(OptionFlag.REQUIRES_ASSET_RELOAD)
                .setStorageHandler(this.saveMainConfigStorageObject);
    }

    private void registerAdvancedPage(ConfigBuilder builder, OptionPageBuilder page) {
        ResourceLocation banner = ResourceLocation.parse("bbe:optimize.banner");
        ResourceLocation chest = ResourceLocation.parse("bbe:optimize.chest");

        page.addOptionGroup(builder.createOptionGroup()
                .addOption(
                        builder.createIntegerOption(ResourceLocation.parse("bbe:misc.banner_pose"))
                                .setName(Component.translatable("bbe.config.storage.main.misc.banner_pose"))
                                .setTooltip(Component.translatable("bbe.config.storage.main.misc.banner_pose.tooltip"))
                                .setDefaultValue(1)
                                .setImpact(OptionImpact.LOW)
                                .setBinding(
                                        value -> GlobalScope.CONFIG.MAIN.setOption("misc.banner_pose", value),
                                        () -> (int) GlobalScope.CONFIG.MAIN.getOption("misc.banner_pose").getValue()
                                )
                                .setRange(new Range(1, 9, 1))
                                .setValueFormatter(value -> {
                                    float degrees = Math.clamp(-0.45f * value, -4.05f, -0.45f);
                                    return Component.literal(String.format(java.util.Locale.ROOT, "%.2f deg", Math.abs(degrees)));
                                })
                                .setEnabledProvider(c -> c.readBooleanOption(MASTER) && c.readBooleanOption(banner), MASTER, banner)
                                .setFlags(OptionFlag.REQUIRES_ASSET_RELOAD)
                                .setStorageHandler(this.saveMainConfigStorageObject)
                )
                .addOption(
                        builder.createEnumOption(ResourceLocation.parse("bbe:misc.banner_graphics"), EnumTypes.BannerGraphicsType.class)
                                .setName(Component.translatable("bbe.config.storage.main.misc.banner_graphics"))
                                .setTooltip(Component.translatable("bbe.config.storage.main.misc.banner_graphics.tooltip"))
                                .setDefaultValue(EnumTypes.BannerGraphicsType.FANCY)
                                .setImpact(OptionImpact.VARIES)
                                .setBinding(
                                        value -> GlobalScope.CONFIG.MAIN.setOption("misc.banner_graphics", EnumTypes.BannerGraphicsType.map(value)),
                                        () -> EnumTypes.BannerGraphicsType.map((int) GlobalScope.CONFIG.MAIN.getOption("misc.banner_graphics").getValue())
                                )
                                .setElementNameProvider(e -> new Component[]{
                                        Component.translatable("bbe.config.storage.main.misc.banner_graphics.type.fast"),
                                        Component.translatable("bbe.config.storage.main.misc.banner_graphics.type.fancy")
                                }[e.ordinal()])
                                .setEnabledProvider(c -> c.readBooleanOption(MASTER) && c.readBooleanOption(banner), MASTER, banner)
                                .setFlags(OptionFlag.REQUIRES_ASSET_RELOAD)
                                .setStorageHandler(this.saveMainConfigStorageObject)
                )
                .addOption(
                        builder.createBooleanOption(ResourceLocation.parse("bbe:misc.christmas_chest"))
                                .setName(Component.translatable("bbe.config.storage.main.misc.christmas_chest"))
                                .setTooltip(Component.translatable("bbe.config.storage.main.misc.christmas_chest.tooltip"))
                                .setDefaultValue(false)
                                .setImpact(OptionImpact.LOW)
                                .setBinding(
                                        value -> GlobalScope.CONFIG.MAIN.setOption("misc.christmas_chest", value),
                                        () -> (boolean) GlobalScope.CONFIG.MAIN.getOption("misc.christmas_chest").getValue()
                                )
                                .setEnabledProvider(c -> c.readBooleanOption(MASTER) && c.readBooleanOption(chest), MASTER, chest)
                                .setFlags(OptionFlag.REQUIRES_ASSET_RELOAD)
                                .setStorageHandler(this.saveMainConfigStorageObject)
                ));
    }

    @Override
    public void registerConfigLate(ConfigBuilder builder) {
        OptionPageBuilder generalPage = builder.createOptionPage()
                .setName(Component.translatable("bbe.config.sodium.pagetext.general"));
        OptionPageBuilder optimizationPage = builder.createOptionPage()
                .setName(Component.translatable("bbe.config.sodium.pagetext.optimizations"));
        OptionPageBuilder animationPage = builder.createOptionPage()
                .setName(Component.translatable("bbe.config.sodium.pagetext.animations"));
        OptionPageBuilder advancedPage = builder.createOptionPage()
                .setName(Component.translatable("bbe.config.sodium.pagetext.advanced"));

        registerGeneralPage(builder, generalPage);
        registerOptimizationPage(builder, optimizationPage);
        registerAnimationPage(builder, animationPage);
        registerAdvancedPage(builder, advancedPage);

        builder.registerOwnModOptions()
                .setNonTintedIcon(ResourceLocation.parse("betterblockentities:icon.png"))
                .setColorTheme(builder.createColorTheme().setBaseThemeRGB(0xc68d46))
                .addPage(generalPage)
                .addPage(optimizationPage)
                .addPage(animationPage)
                .addPage(advancedPage);
    }
}
