package betterblockentities.client.model.overrides;

/* local */
import betterblockentities.client.gui.config.ConfigCache;
import betterblockentities.client.gui.option.EnumTypes;

/* minecraft */
import net.minecraft.client.model.geom.ModelPart;

public final class ChestModelOverride {
    public static void splitModel(final ModelPart bottom) {
        if (ConfigCache.updateType != EnumTypes.UpdateSchedulerType.SMART.ordinal()) {
            bottom.skipDraw = true;
            bottom.visible = false;
        }
    }
}
