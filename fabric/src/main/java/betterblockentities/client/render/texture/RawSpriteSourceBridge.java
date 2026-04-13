package betterblockentities.client.render.texture;

/* minecraft */
import net.minecraft.client.renderer.texture.atlas.SpriteSource;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

/* java */
import java.util.Map;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public final class RawSpriteSourceBridge {
    private static final Method OUTPUT_ADD_METHOD;

    private RawSpriteSourceBridge() {}

    public static void addDirectory(
            ResourceManager resourceManager,
            SpriteSource.Output output,
            String sourcePath,
            String idPrefix
    ) {
        FileToIdConverter converter = new FileToIdConverter(sourcePath, ".png");
        Map<Identifier, Resource> resources = converter.listMatchingResources(resourceManager);

        resources.forEach((fileId, resource) ->
                addSprite(output, converter.fileToId(fileId).withPrefix(idPrefix), resource));
    }

    private static void addSprite(SpriteSource.Output output, Identifier spriteId, Resource resource) {
        try {
            OUTPUT_ADD_METHOD.invoke(output, spriteId, (SpriteSource.DiscardableLoader) loader -> loader.loadSprite(spriteId, resource));
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException("Failed to register sprite " + spriteId, e);
        }
    }

    static {
        try {
            OUTPUT_ADD_METHOD = SpriteSource.Output.class.getMethod("add", Identifier.class, SpriteSource.DiscardableLoader.class);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }
}
