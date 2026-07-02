package betterblockentities.platform;

/* neoforge */
import net.neoforged.fml.ModList;

/* java/misc */
import java.util.List;
import java.util.ServiceLoader;

public class NeoForgePlatformAccess implements PlatformInterface {
    @Override
    public boolean isModLoaded(String modIdentifier) {
        return ModList.get().isLoaded(modIdentifier);
    }

    @Override
    public <T> List<T> getEntryPoints(Class<T> service, String key) {
        return ServiceLoader
                .load(service)
                .stream()
                .map(ServiceLoader.Provider::get)
                .toList();
    }
}
