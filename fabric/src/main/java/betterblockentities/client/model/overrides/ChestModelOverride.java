package betterblockentities.client.model.overrides;

/* local */
import betterblockentities.client.gui.config.ConfigCache;
import betterblockentities.client.gui.option.EnumTypes;

/* minecraft */
import net.minecraft.client.model.geom.ModelPart;

public final class ChestModelOverride {
    public static void applyOptimization(final ModelPart bottom) {
        if (ConfigCache.updateType != EnumTypes.UpdateSchedulerType.SMART.ordinal()) {
            bottom.skipDraw = true;
            bottom.visible = false;
            return;
        }

        bottom.skipDraw = false;
        bottom.visible = true;
    }

    public static void clearOptimization(final ModelPart bottom) {
        bottom.skipDraw = false;
        bottom.visible = true;
    }
}
