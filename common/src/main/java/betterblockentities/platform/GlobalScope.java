package betterblockentities.platform;

/* local */
import betterblockentities.client.gui.config.BBEConfig;
import betterblockentities.client.gui.config.wrapper.GenericConfigWrapper;
import betterblockentities.registration.AltRendererRegistration;
import betterblockentities.registration.BBEApiEntryPoint;
import betterblockentities.render.AltRenderDispatcher;

/* minecraft */
import net.minecraft.client.gui.components.debug.DebugEntryCategory;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.culling.Frustum;

/* java/misc */
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import com.google.common.collect.ImmutableList;

public class GlobalScope {
    /* platform */
    public static PlatformInterface platformAccess = null;
    public static final String MOD_ID = "betterblockentities";

    /* debug */
    public static final Logger LOGGER = LoggerFactory.getLogger("BBE-Logger");
    public static DebugEntryCategory DEBUG_CATEGORY = new DebugEntryCategory(Component.literal("BBE"), 10F);
    public static Identifier DEBUG_ID = Identifier.fromNamespaceAndPath("bbe", "debug");

    /* config */
    public static final BBEConfig CONFIG = new BBEConfig();
    public static final GenericConfigWrapper OPTIONS = new GenericConfigWrapper();

    /* runtime render data */
    public static Frustum frustum;
    public static AltRenderDispatcher altRenderDispatcher;
    public static List<BlockEntityRenderState> altBlockEntityRenderStates = new ArrayList<>();
    public static boolean limitVanillaSignRendering = false;

    public static class ApiInternal {
        public static final String ENTRYPOINT_KEY = "bbe:renderer_registration_api";

        public static void collectEntryPoints() {
            if (platformAccess == null) {
                throw new IllegalStateException("No platform access has been assigned!");
            }

            List<BBEApiEntryPoint> entrypoints = platformAccess.getEntryPoints(BBEApiEntryPoint.class, ENTRYPOINT_KEY);

            AltRendererRegistration context = new AltRendererRegistration();

            GlobalScope.LOGGER.info("Registering alt renderers via each collected entrypoint...");

            for (BBEApiEntryPoint entrypoint : entrypoints) {
                try {
                    entrypoint.registerRenderers(context);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
