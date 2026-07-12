package betterblockentities.platform;

/* java/misc */
import java.util.List;

public interface PlatformInterface {
    default boolean isModLoaded(String modIdentifier) {
        throw new UnsupportedOperationException("Platform specific overload needs to be implemented!");
    }

    default <T> List<T> getEntryPoints(Class<T> service, String key) {
        throw new UnsupportedOperationException("Platform specific overload needs to be implemented!");
    }
}
