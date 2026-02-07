package betterblockentities.client.render.texture;

/* minecraft */
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.atlas.SpriteResourceLoader;
import net.minecraft.client.renderer.texture.atlas.SpriteSource;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;

/* java */
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.util.Map;

public final class RawSpriteSourceBridge {
    private static final Method OUTPUT_ADD_METHOD;
    private static final MethodHandle LOAD_SPRITE_HANDLE;

    private RawSpriteSourceBridge() {}

    public static void addDirectory(
            ResourceManager resourceManager,
            SpriteSource.Output output,
            String sourcePath,
            String idPrefix
    ) {
        FileToIdConverter converter = new FileToIdConverter(sourcePath, ".png");
        Map<Identifier, Object> resources = (Map<Identifier, Object>) (Map<?, ?>) converter.listMatchingResources(resourceManager);

        resources.forEach((fileId, resource) ->
                addSprite(output, converter.fileToId(fileId).withPrefix(idPrefix), resource));
    }

    private static void addSprite(SpriteSource.Output output, Identifier spriteId, Object resource) {
        try {
            SpriteSource.DiscardableLoader loader = spriteLoader -> {
                try {
                    return (SpriteContents) LOAD_SPRITE_HANDLE.invokeWithArguments(spriteLoader, spriteId, resource);
                } catch (Throwable e) {
                    throw new RuntimeException("Failed to load sprite " + spriteId, e);
                }
            };
            OUTPUT_ADD_METHOD.invoke(output, spriteId, loader);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to register sprite " + spriteId, e);
        }
    }

    static {
        try {
            Class<?> resourceClass = Class.forName("net.minecraft.server.packs.resources.Resource");
            OUTPUT_ADD_METHOD = SpriteSource.Output.class.getMethod("add", Identifier.class, SpriteSource.DiscardableLoader.class);
            LOAD_SPRITE_HANDLE = MethodHandles.publicLookup().findVirtual(
                    SpriteResourceLoader.class,
                    "loadSprite",
                    MethodType.methodType(SpriteContents.class, Identifier.class, resourceClass)
            );
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }
}
