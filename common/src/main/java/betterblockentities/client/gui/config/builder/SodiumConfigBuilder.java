package betterblockentities.client.gui.config.builder;

/* local */
import betterblockentities.client.gui.option.EnumTypes;
import betterblockentities.client.gui.storage.SodiumConfigStorage;

/* sodium */
import betterblockentities.platform.GlobalScope;
import net.caffeinemc.mods.sodium.api.config.ConfigEntryPoint;
import net.caffeinemc.mods.sodium.api.config.StorageEventHandler;
import net.caffeinemc.mods.sodium.api.config.option.OptionFlag;
import net.caffeinemc.mods.sodium.api.config.option.OptionImpact;
import net.caffeinemc.mods.sodium.api.config.option.Range;
import net.caffeinemc.mods.sodium.api.config.structure.ConfigBuilder;
import net.caffeinemc.mods.sodium.api.config.structure.OptionPageBuilder;

/* minecraft */
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/* java/misc */
import java.util.Locale;

public class SodiumConfigBuilder implements ConfigEntryPoint {
    private final SodiumConfigStorage bbeSodiumConfigStorage = new SodiumConfigStorage();
    private final StorageEventHandler saveMainConfigStorageObject = bbeSodiumConfigStorage::save;


    public void registerGeneralPage(ConfigBuilder builder, OptionPageBuilder generalPage) {
        generalPage.addOptionGroup(builder.createOptionGroup()
                .addOption(
                        builder.createBooleanOption(Identifier.parse("bbe:master"))
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
        generalPage.addOptionGroup(builder.createOptionGroup()
                .addOption(
                        builder.createEnumOption(Identifier.parse("bbe:misc.shademode"), EnumTypes.ShadeMode.class)
                                .setName(Component.translatable("bbe.config.storage.main.misc.shademode"))
                                .setTooltip(
                                        Component.translatable("bbe.config.storage.main.misc.shademode.tooltip")
                                )
                                .setDefaultValue(EnumTypes.ShadeMode.SODIUM)
                                .setImpact(OptionImpact.VARIES)
                                .setBinding(
                                        value -> GlobalScope.CONFIG.MAIN.setOption("misc.shademode", EnumTypes.ShadeMode.map(value)),
                                        () -> EnumTypes.ShadeMode.map((int)GlobalScope.CONFIG.MAIN.getOption("misc.shademode").getValue())
                                )
                                .setElementNameProvider(e -> new Component[]{
                                        Component.translatable("bbe.config.storage.main.misc.shademode.type.sodium"),
                                        Component.translatable("bbe.config.storage.main.misc.shademode.type.vanilla"),
                                }[e.ordinal()])
                                .setEnabledProvider(c ->
                                        c.readBooleanOption(Identifier.parse("bbe:master")), Identifier.parse("bbe:master")
                                )
                                .setFlags(OptionFlag.REQUIRES_ASSET_RELOAD)
                                .setStorageHandler(this.saveMainConfigStorageObject)
                )
                .addOption(
                        builder.createEnumOption(Identifier.parse("bbe:misc.update_scheduler"), EnumTypes.UpdateSchedulerType.class)
                                .setName(Component.translatable("bbe.config.storage.main.misc.update_scheduler"))
                                .setTooltip(
                                        Component.translatable("bbe.config.storage.main.misc.update_scheduler.tooltip")
                                )
                                .setDefaultValue(EnumTypes.UpdateSchedulerType.FAST)
                                .setImpact(OptionImpact.VARIES)
                                .setBinding(
                                        value -> GlobalScope.CONFIG.MAIN.setOption("misc.update_scheduler", EnumTypes.UpdateSchedulerType.map(value)),
                                        () -> EnumTypes.UpdateSchedulerType.map((int)GlobalScope.CONFIG.MAIN.getOption("misc.update_scheduler").getValue())
                                )
                                .setElementNameProvider(e -> new Component[]{
                                        Component.translatable("bbe.config.storage.main.misc.update_scheduler.type.fast"),
                                        Component.translatable("bbe.config.storage.main.misc.update_scheduler.type.smart"),
                                }[e.ordinal()])
                                .setEnabledProvider(c ->
                                        c.readBooleanOption(Identifier.parse("bbe:master")), Identifier.parse("bbe:master")
                                )
                                .setFlags(OptionFlag.REQUIRES_ASSET_RELOAD)
                                .setStorageHandler(this.saveMainConfigStorageObject)
                )
        );
    }

    public void registerOptimizationPage(ConfigBuilder builder, OptionPageBuilder optimizationPage) {
        optimizationPage.addOptionGroup(builder.createOptionGroup()
                .addOption(
                        builder.createBooleanOption(Identifier.parse("bbe:optimize.sign"))
                                .setName(Component.translatable("bbe.config.storage.main.optimize.sign"))
                                .setTooltip(Component.translatable("bbe.config.storage.main.optimize.sign.tooltip"))
                                .setDefaultValue(true)
                                .setImpact(OptionImpact.HIGH)
                                .setBinding(
                                        value -> GlobalScope.CONFIG.MAIN.setOption("optimize.sign", value),
                                        () -> (boolean) GlobalScope.CONFIG.MAIN.getOption("optimize.sign").getValue()
                                )
                                .setEnabledProvider(c ->
                                        c.readBooleanOption(Identifier.parse("bbe:master")), Identifier.parse("bbe:master")
                                )
                                .setFlags(OptionFlag.REQUIRES_ASSET_RELOAD)
                                .setStorageHandler(this.saveMainConfigStorageObject)
                )
                .addOption(
                        builder.createBooleanOption(Identifier.parse("bbe:misc.sign_text"))
                                .setName(Component.translatable("bbe.config.storage.main.misc.sign_text"))
                                .setTooltip(Component.translatable("bbe.config.storage.main.misc.sign_text.tooltip"))
                                .setDefaultValue(true)
                                .setImpact(OptionImpact.MEDIUM)
                                .setBinding(
                                        value -> GlobalScope.CONFIG.MAIN.setOption("misc.sign_text", value),
                                        () -> (boolean) GlobalScope.CONFIG.MAIN.getOption("misc.sign_text").getValue()
                                )
                                .setEnabledProvider(c ->
                                                c.readBooleanOption(Identifier.parse("bbe:master")) &&
                                                        c.readBooleanOption(Identifier.parse("bbe:optimize.sign")),
                                        Identifier.parse("bbe:master"), Identifier.parse("bbe:optimize.sign")
                                )
                                .setFlags(OptionFlag.REQUIRES_ASSET_RELOAD)
                                .setStorageHandler(this.saveMainConfigStorageObject)
                )
                .addOption(
                        builder.createIntegerOption(Identifier.parse("bbe:misc.sign_text_distance"))
                                .setName(Component.translatable("bbe.config.storage.main.misc.sign_text_distance"))
                                .setTooltip(Component.translatable("bbe.config.storage.main.misc.sign_text_distance.tooltip"))
                                .setDefaultValue(16)
                                .setImpact(OptionImpact.MEDIUM)
                                .setBinding(
                                        value -> GlobalScope.CONFIG.MAIN.setOption("misc.sign_text_distance", value),
                                        () -> (int) GlobalScope.CONFIG.MAIN.getOption("misc.sign_text_distance").getValue()
                                )
                                .setRange(new Range(1, 64, 1))
                                .setValueFormatter((v) -> Component.literal(v + " blocks"))
                                .setEnabledProvider(c ->
                                                c.readBooleanOption(Identifier.parse("bbe:master")) &&
                                                        c.readBooleanOption(Identifier.parse("bbe:optimize.sign")) &&
                                                        c.readBooleanOption(Identifier.parse("bbe:misc.sign_text")),
                                        Identifier.parse("bbe:master"), Identifier.parse("bbe:optimize.sign"),
                                        Identifier.parse("bbe:misc.sign_text")
                                )
                                .setFlags(OptionFlag.REQUIRES_ASSET_RELOAD)
                                .setStorageHandler(this.saveMainConfigStorageObject)
                )
                .addOption(
                        builder.createBooleanOption(Identifier.parse("bbe:misc.sign_text_culling"))
                                .setName(Component.translatable("bbe.config.storage.main.misc.sign_text_culling"))
                                .setTooltip(Component.translatable("bbe.config.storage.main.misc.sign_text_culling.tooltip"))
                                .setDefaultValue(true)
                                .setImpact(OptionImpact.MEDIUM)
                                .setBinding(
                                        value -> GlobalScope.CONFIG.MAIN.setOption("misc.sign_text_culling", value),
                                        () -> (boolean) GlobalScope.CONFIG.MAIN.getOption("misc.sign_text_culling").getValue()
                                )
                                .setEnabledProvider(c ->
                                                c.readBooleanOption(Identifier.parse("bbe:master")) &&
                                                        c.readBooleanOption(Identifier.parse("bbe:optimize.sign")) &&
                                                        c.readBooleanOption(Identifier.parse("bbe:misc.sign_text")),
                                        Identifier.parse("bbe:master"), Identifier.parse("bbe:optimize.sign"),
                                        Identifier.parse("bbe:misc.sign_text")
                                )
                                .setFlags(OptionFlag.REQUIRES_ASSET_RELOAD)
                                .setStorageHandler(this.saveMainConfigStorageObject)
                )
        );

        optimizationPage.addOptionGroup(builder.createOptionGroup()
                .addOption(
                        builder.createBooleanOption(Identifier.parse("bbe:optimize.chest"))
                                .setName(Component.translatable("bbe.config.storage.main.optimize.chest"))
                                .setTooltip(Component.translatable("bbe.config.storage.main.optimize.chest.tooltip"))
                                .setDefaultValue(true)
                                .setImpact(OptionImpact.HIGH)
                                .setBinding(
                                        value -> GlobalScope.CONFIG.MAIN.setOption("optimize.chest", value),
                                        () -> (boolean) GlobalScope.CONFIG.MAIN.getOption("optimize.chest").getValue()
                                )
                                .setEnabledProvider(c ->
                                        c.readBooleanOption(Identifier.parse("bbe:master")), Identifier.parse("bbe:master")
                                )
                                .setFlags(OptionFlag.REQUIRES_ASSET_RELOAD)
                                .setStorageHandler(this.saveMainConfigStorageObject)
                )
                .addOption(
                        builder.createBooleanOption(Identifier.parse("bbe:optimize.banner"))
                                .setName(Component.translatable("bbe.config.storage.main.optimize.banner"))
                                .setTooltip(Component.translatable("bbe.config.storage.main.optimize.banner.tooltip"))
                                .setDefaultValue(true)
                                .setImpact(OptionImpact.HIGH)
                                .setBinding(
                                        value -> GlobalScope.CONFIG.MAIN.setOption("optimize.banner", value),
                                        () -> (boolean) GlobalScope.CONFIG.MAIN.getOption("optimize.banner").getValue()
                                )
                                .setEnabledProvider(c ->
                                        c.readBooleanOption(Identifier.parse("bbe:master")), Identifier.parse("bbe:master")
                                )
                                .setFlags(OptionFlag.REQUIRES_ASSET_RELOAD)
                                .setStorageHandler(this.saveMainConfigStorageObject)
                )
                .addOption(
                        builder.createBooleanOption(Identifier.parse("bbe:optimize.shulker"))
                                .setName(Component.translatable("bbe.config.storage.main.optimize.shulker"))
                                .setTooltip(Component.translatable("bbe.config.storage.main.optimize.shulker.tooltip"))
                                .setDefaultValue(true)
                                .setImpact(OptionImpact.HIGH)
                                .setBinding(
                                        value -> GlobalScope.CONFIG.MAIN.setOption("optimize.shulker", value),
                                        () -> (boolean) GlobalScope.CONFIG.MAIN.getOption("optimize.shulker").getValue()
                                )
                                .setEnabledProvider(c ->
                                        c.readBooleanOption(Identifier.parse("bbe:master")), Identifier.parse("bbe:master")
                                )
                                .setFlags(OptionFlag.REQUIRES_ASSET_RELOAD)
                                .setStorageHandler(this.saveMainConfigStorageObject)
                )
                .addOption(
                        builder.createBooleanOption(Identifier.parse("bbe:optimize.decoratedpot"))
                                .setName(Component.translatable("bbe.config.storage.main.optimize.decoratedpot"))
                                .setTooltip(Component.translatable("bbe.config.storage.main.optimize.decoratedpot.tooltip"))
                                .setDefaultValue(true)
                                .setImpact(OptionImpact.HIGH)
                                .setBinding(
                                        value -> GlobalScope.CONFIG.MAIN.setOption("optimize.decoratedpot", value),
                                        () -> (boolean) GlobalScope.CONFIG.MAIN.getOption("optimize.decoratedpot").getValue()
                                )
                                .setEnabledProvider(c ->
                                        c.readBooleanOption(Identifier.parse("bbe:master")), Identifier.parse("bbe:master")
                                )
                                .setFlags(OptionFlag.REQUIRES_ASSET_RELOAD)
                                .setStorageHandler(this.saveMainConfigStorageObject)
                )
                .addOption(
                        builder.createBooleanOption(Identifier.parse("bbe:optimize.bell"))
                                .setName(Component.translatable("bbe.config.storage.main.optimize.bell"))
                                .setTooltip(Component.translatable("bbe.config.storage.main.optimize.bell.tooltip"))
                                .setDefaultValue(true)
                                .setImpact(OptionImpact.HIGH)
                                .setBinding(
                                        value -> GlobalScope.CONFIG.MAIN.setOption("optimize.bell", value),
                                        () -> (boolean) GlobalScope.CONFIG.MAIN.getOption("optimize.bell").getValue()
                                )
                                .setEnabledProvider(c ->
                                        c.readBooleanOption(Identifier.parse("bbe:master")), Identifier.parse("bbe:master")
                                )
                                .setFlags(OptionFlag.REQUIRES_ASSET_RELOAD)
                                .setStorageHandler(this.saveMainConfigStorageObject)
                )
                .addOption(
                        builder.createBooleanOption(Identifier.parse("bbe:optimize.copper_golem_statue"))
                                .setName(Component.translatable("bbe.config.storage.main.optimize.copper_golem_statue"))
                                .setTooltip(Component.translatable("bbe.config.storage.main.optimize.copper_golem_statue.tooltip"))
                                .setDefaultValue(true)
                                .setImpact(OptionImpact.HIGH)
                                .setBinding(
                                        value -> GlobalScope.CONFIG.MAIN.setOption("optimize.copper_golem_statue", value),
                                        () -> (boolean) GlobalScope.CONFIG.MAIN.getOption("optimize.copper_golem_statue").getValue()
                                )
                                .setEnabledProvider(c ->
                                        c.readBooleanOption(Identifier.parse("bbe:master")), Identifier.parse("bbe:master")
                                )
                                .setFlags(OptionFlag.REQUIRES_ASSET_RELOAD)
                                .setStorageHandler(this.saveMainConfigStorageObject)
                )
                .addOption(
                        builder.createBooleanOption(Identifier.parse("bbe:optimize.shelf"))
                                .setName(Component.translatable("bbe.config.storage.main.optimize.shelf"))
                                .setTooltip(Component.translatable("bbe.config.storage.main.optimize.shelf.tooltip"))
                                .setDefaultValue(true)
                                .setImpact(OptionImpact.HIGH)
                                .setBinding(
                                        value -> GlobalScope.CONFIG.MAIN.setOption("optimize.shelf", value),
                                        () -> (boolean) GlobalScope.CONFIG.MAIN.getOption("optimize.shelf").getValue()
                                )
                                .setEnabledProvider(c ->
                                        c.readBooleanOption(Identifier.parse("bbe:master")), Identifier.parse("bbe:master")
                                )
                                .setFlags(OptionFlag.REQUIRES_ASSET_RELOAD)
                                .setStorageHandler(this.saveMainConfigStorageObject)
                )
                .addOption(
                        builder.createBooleanOption(Identifier.parse("bbe:optimize.campfire"))
                                .setName(Component.translatable("bbe.config.storage.main.optimize.campfire"))
                                .setTooltip(Component.translatable("bbe.config.storage.main.optimize.campfire.tooltip"))
                                .setDefaultValue(true)
                                .setImpact(OptionImpact.HIGH)
                                .setBinding(
                                        value -> GlobalScope.CONFIG.MAIN.setOption("optimize.campfire", value),
                                        () -> (boolean) GlobalScope.CONFIG.MAIN.getOption("optimize.campfire").getValue()
                                )
                                .setEnabledProvider(c ->
                                        c.readBooleanOption(Identifier.parse("bbe:master")), Identifier.parse("bbe:master")
                                )
                                .setFlags(OptionFlag.REQUIRES_ASSET_RELOAD)
                                .setStorageHandler(this.saveMainConfigStorageObject)
                )
                .addOption(
                        builder.createBooleanOption(Identifier.parse("bbe:optimize.lectern"))
                                .setName(Component.translatable("bbe.config.storage.main.optimize.lectern"))
                                .setTooltip(Component.translatable("bbe.config.storage.main.optimize.lectern.tooltip"))
                                .setDefaultValue(true)
                                .setImpact(OptionImpact.HIGH)
                                .setBinding(
                                        value -> GlobalScope.CONFIG.MAIN.setOption("optimize.lectern", value),
                                        () -> (boolean) GlobalScope.CONFIG.MAIN.getOption("optimize.lectern").getValue()
                                )
                                .setEnabledProvider(c ->
                                        c.readBooleanOption(Identifier.parse("bbe:master")), Identifier.parse("bbe:master")
                                )
                                .setFlags(OptionFlag.REQUIRES_ASSET_RELOAD)
                                .setStorageHandler(this.saveMainConfigStorageObject)
                )
        );
    }

    public void registerAnimationPage(ConfigBuilder builder, OptionPageBuilder animationPage) {
        animationPage.addOptionGroup(builder.createOptionGroup()
                .addOption(
                        builder.createBooleanOption(Identifier.parse("bbe:animation.chest"))
                                .setName(Component.translatable("bbe.config.storage.main.animation.chest"))
                                .setTooltip(Component.translatable("bbe.config.storage.main.animation.chest.tooltip"))
                                .setDefaultValue(true)
                                .setImpact(OptionImpact.LOW)
                                .setBinding(
                                        value -> GlobalScope.CONFIG.MAIN.setOption("animation.chest", value),
                                        () -> (boolean) GlobalScope.CONFIG.MAIN.getOption("animation.chest").getValue()
                                )
                                .setEnabledProvider(c ->
                                                c.readBooleanOption(Identifier.parse("bbe:master")) &&
                                                        c.readBooleanOption(Identifier.parse("bbe:optimize.chest")),
                                        Identifier.parse("bbe:master"), Identifier.parse("bbe:optimize.chest")
                                )
                                .setFlags(OptionFlag.REQUIRES_ASSET_RELOAD)
                                .setStorageHandler(this.saveMainConfigStorageObject)
                )
                .addOption(
                        builder.createBooleanOption(Identifier.parse("bbe:animation.shulker"))
                                .setName(Component.translatable("bbe.config.storage.main.animation.shulker"))
                                .setTooltip(Component.translatable("bbe.config.storage.main.animation.shulker.tooltip"))
                                .setDefaultValue(true)
                                .setImpact(OptionImpact.LOW)
                                .setBinding(
                                        value -> GlobalScope.CONFIG.MAIN.setOption("animation.shulker", value),
                                        () -> (boolean) GlobalScope.CONFIG.MAIN.getOption("animation.shulker").getValue()
                                )
                                .setEnabledProvider(c ->
                                                c.readBooleanOption(Identifier.parse("bbe:master")) &&
                                                        c.readBooleanOption(Identifier.parse("bbe:optimize.shulker")),
                                        Identifier.parse("bbe:master"), Identifier.parse("bbe:optimize.shulker")
                                )
                                .setFlags(OptionFlag.REQUIRES_ASSET_RELOAD)
                                .setStorageHandler(this.saveMainConfigStorageObject)
                )
                .addOption(
                        builder.createBooleanOption(Identifier.parse("bbe:animation.decoratedpot"))
                                .setName(Component.translatable("bbe.config.storage.main.animation.decoratedpot"))
                                .setTooltip(Component.translatable("bbe.config.storage.main.animation.decoratedpot.tooltip"))
                                .setDefaultValue(true)
                                .setImpact(OptionImpact.LOW)
                                .setBinding(
                                        value -> GlobalScope.CONFIG.MAIN.setOption("animation.decoratedpot", value),
                                        () -> (boolean) GlobalScope.CONFIG.MAIN.getOption("animation.decoratedpot").getValue()
                                )
                                .setEnabledProvider(c ->
                                                c.readBooleanOption(Identifier.parse("bbe:master")) &&
                                                        c.readBooleanOption(Identifier.parse("bbe:optimize.decoratedpot")),
                                        Identifier.parse("bbe:master"), Identifier.parse("bbe:optimize.decoratedpot")
                                )
                                .setFlags(OptionFlag.REQUIRES_ASSET_RELOAD)
                                .setStorageHandler(this.saveMainConfigStorageObject)
                )
                .addOption(
                        builder.createBooleanOption(Identifier.parse("bbe:animation.bell"))
                                .setName(Component.translatable("bbe.config.storage.main.animation.bell"))
                                .setTooltip(Component.translatable("bbe.config.storage.main.animation.bell.tooltip"))
                                .setDefaultValue(true)
                                .setImpact(OptionImpact.LOW)
                                .setBinding(
                                        value -> GlobalScope.CONFIG.MAIN.setOption("animation.bell", value),
                                        () -> (boolean) GlobalScope.CONFIG.MAIN.getOption("animation.bell").getValue()
                                )
                                .setEnabledProvider(c ->
                                                c.readBooleanOption(Identifier.parse("bbe:master")) &&
                                                        c.readBooleanOption(Identifier.parse("bbe:optimize.bell")),
                                        Identifier.parse("bbe:master"), Identifier.parse("bbe:optimize.bell")
                                )
                                .setFlags(OptionFlag.REQUIRES_ASSET_RELOAD)
                                .setStorageHandler(this.saveMainConfigStorageObject)
                )
        );
    }

    public void registerAdvancedPage(ConfigBuilder builder, OptionPageBuilder advancedPage) {
        advancedPage.addOptionGroup(builder.createOptionGroup()
                .addOption(
                        builder.createIntegerOption(Identifier.parse("bbe:misc.banner_pose"))
                                .setName(Component.translatable("bbe.config.storage.main.misc.banner_pose"))
                                .setTooltip(Component.translatable("bbe.config.storage.main.misc.banner_pose.tooltip"))
                                .setDefaultValue(1)
                                .setImpact(OptionImpact.LOW)
                                .setBinding(
                                        value -> GlobalScope.CONFIG.MAIN.setOption("misc.banner_pose", value),
                                        () -> (int) GlobalScope.CONFIG.MAIN.getOption("misc.banner_pose").getValue()
                                )
                                .setRange(new Range(1, 9, 1))
                                .setValueFormatter(v -> {
                                    float degrees = Math.clamp(-0.45f * v, -4.05f, -0.45f);
                                    float absDegrees = Math.abs(degrees);
                                    return Component.literal(String.format(Locale.ROOT, "%.2f deg", absDegrees));
                                })
                                .setEnabledProvider(c ->
                                                c.readBooleanOption(Identifier.parse("bbe:master")) &&
                                                        c.readBooleanOption(Identifier.parse("bbe:optimize.banner")),
                                        Identifier.parse("bbe:master"), Identifier.parse("bbe:optimize.banner")
                                )
                                .setFlags(OptionFlag.REQUIRES_ASSET_RELOAD)
                                .setStorageHandler(this.saveMainConfigStorageObject)
                )
                .addOption(
                        builder.createEnumOption(Identifier.parse("bbe:misc.banner_graphics"), EnumTypes.BannerGraphicsType.class)
                                .setName(Component.translatable("bbe.config.storage.main.misc.banner_graphics"))
                                .setTooltip(Component.translatable("bbe.config.storage.main.misc.banner_graphics.tooltip"))
                                .setDefaultValue(EnumTypes.BannerGraphicsType.FANCY)
                                .setImpact(OptionImpact.VARIES)
                                .setBinding(
                                        value -> GlobalScope.CONFIG.MAIN.setOption("misc.banner_graphics", EnumTypes.BannerGraphicsType.map(value)),
                                        () -> EnumTypes.BannerGraphicsType.map((int)GlobalScope.CONFIG.MAIN.getOption("misc.banner_graphics").getValue())
                                )
                                .setElementNameProvider(e -> new Component[]{
                                        Component.translatable("bbe.config.storage.main.misc.banner_graphics.type.fast"),
                                        Component.translatable("bbe.config.storage.main.misc.banner_graphics.type.fancy"),
                                }[e.ordinal()])
                                .setEnabledProvider(c ->
                                                c.readBooleanOption(Identifier.parse("bbe:master")) &&
                                                        c.readBooleanOption(Identifier.parse("bbe:optimize.banner")),
                                        Identifier.parse("bbe:master"), Identifier.parse("bbe:optimize.banner")
                                )
                                .setFlags(OptionFlag.REQUIRES_ASSET_RELOAD)
                                .setStorageHandler(this.saveMainConfigStorageObject)
                )
                .addOption(
                        builder.createBooleanOption(Identifier.parse("bbe:misc.christmas_chest"))
                                .setName(Component.translatable("bbe.config.storage.main.misc.christmas_chest"))
                                .setTooltip(Component.translatable("bbe.config.storage.main.misc.christmas_chest.tooltip"))
                                .setDefaultValue(false)
                                .setImpact(OptionImpact.LOW)
                                .setBinding(
                                        value -> GlobalScope.CONFIG.MAIN.setOption("misc.christmas_chest", value),
                                        () -> (boolean) GlobalScope.CONFIG.MAIN.getOption("misc.christmas_chest").getValue()
                                )
                                .setEnabledProvider(c ->
                                                c.readBooleanOption(Identifier.parse("bbe:master")) &&
                                                        c.readBooleanOption(Identifier.parse("bbe:optimize.chest")),
                                        Identifier.parse("bbe:master"), Identifier.parse("bbe:optimize.chest")
                                )
                                .setFlags(OptionFlag.REQUIRES_ASSET_RELOAD)
                                .setStorageHandler(this.saveMainConfigStorageObject)
                )
        );
    }

    @Override
    public void registerConfigLate(ConfigBuilder builder) {
        OptionPageBuilder generalPage = builder.createOptionPage().setName(Component.translatable("bbe.config.sodium.pagetext.general"));
        OptionPageBuilder optimizationPage = builder.createOptionPage().setName(Component.translatable("bbe.config.sodium.pagetext.optimizations"));
        OptionPageBuilder animationPage = builder.createOptionPage().setName(Component.translatable("bbe.config.sodium.pagetext.animations"));
        OptionPageBuilder advancedPage = builder.createOptionPage().setName(Component.translatable("bbe.config.sodium.pagetext.advanced"));

        registerGeneralPage(builder, generalPage);
        registerOptimizationPage(builder, optimizationPage);
        registerAnimationPage(builder, animationPage);
        registerAdvancedPage(builder, advancedPage);

        builder.registerOwnModOptions()
                .setNonTintedIcon(Identifier.parse("betterblockentities:icon.png"))
                .setColorTheme(builder.createColorTheme().setBaseThemeRGB(0xc68d46)) //old 0x603900
                .addPage(generalPage)
                .addPage(optimizationPage)
                .addPage(animationPage)
                .addPage(advancedPage);

    }
}
