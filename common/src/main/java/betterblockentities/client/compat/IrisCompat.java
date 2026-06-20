package betterblockentities.client.compat;

/* java/misc */
import java.lang.reflect.Method;
import java.util.function.BooleanSupplier;

public final class IrisCompat {
    private static final BooleanSupplier SHADER_PACK_IN_USE = createShaderPackCheck();

    public static boolean isShaderPackInUse() {
        return SHADER_PACK_IN_USE.getAsBoolean();
    }

    private static BooleanSupplier createShaderPackCheck() {
        try {
            Class<?> apiClass = Class.forName(
                    "net.irisshaders.iris.api.v0.IrisApi",
                    false,
                    IrisCompat.class.getClassLoader()
            );
            Method getInstance = apiClass.getMethod("getInstance");
            Method isShaderPackInUse = apiClass.getMethod("isShaderPackInUse");
            Object api = getInstance.invoke(null);

            return () -> invokeShaderPackCheck(isShaderPackInUse, api);
        } catch (ReflectiveOperationException ignored) {
            return () -> false;
        }
    }

    private static boolean invokeShaderPackCheck(Method method, Object api) {
        try {
            return (boolean)method.invoke(api);
        } catch (ReflectiveOperationException | ClassCastException ignored) {
            return false;
        }
    }
}
