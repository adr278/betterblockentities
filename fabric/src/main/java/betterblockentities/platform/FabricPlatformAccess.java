package betterblockentities.platform;

import net.fabricmc.loader.api.FabricLoader;

import java.util.List;

public class FabricPlatformAccess implements PlatformInterface {
    @Override
    public boolean isModLoaded(String modIdentifier) {
        return FabricLoader.getInstance().isModLoaded(modIdentifier);
    }

    @Override
    public <T> List<T> getEntryPoints(Class<T> service, String key) {
        return FabricLoader.getInstance().getEntrypoints(key, service);
    }
}
