package betterblockentities.registration;

/* local */
import betterblockentities.data.RegistrationInfo;
import betterblockentities.data.ValidationChecks;
import betterblockentities.render.AltRendererProvider;

/* fabric */
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.entrypoint.EntrypointContainer;

/* java/misc */
import java.util.*;

@SuppressWarnings("rawtypes")
public class RegistrationCollection {
    private static final Map<AltRendererProvider, RegistrationInfo> REGISTRATIONS = new HashMap<>();

    public static void addRegistration(AltRendererProvider provider, RegistrationInfo regInfo) {
        if (!ValidationChecks.registrationTypeValid(regInfo)) {
            return;
        }

        REGISTRATIONS.put(provider, regInfo);
    }

    public static Map<AltRendererProvider, RegistrationInfo> getRegistrations() {
        return REGISTRATIONS;
    }
}
