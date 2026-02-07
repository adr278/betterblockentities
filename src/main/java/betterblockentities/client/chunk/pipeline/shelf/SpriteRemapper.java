package betterblockentities.client.chunk.pipeline.shelf;

/* minecraft */
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;

/* mixin */
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

/* java/misc */
import java.util.concurrent.ConcurrentHashMap;

public final class SpriteRemapper {
    private static final Identifier MISSINGNO = Identifier.withDefaultNamespace("missingno");

    // Source sprite -> block atlas sprite (or missing sentinel).
    private final ConcurrentHashMap<TextureAtlasSprite, TextureAtlasSprite> itemToBlock = new ConcurrentHashMap<>();

    public void clear() {
        itemToBlock.clear();
    }

    public @NonNull TextureAtlasSprite missingNoOrNull() {
        return getMissingSprite();
    }

    public boolean isNotBlockAtlas(TextureAtlasSprite sprite) {
        if (sprite == null) return true;
        return sprite.atlasLocation() != AtlasIds.BLOCKS;
    }

    public @Nullable TextureAtlasSprite tryGetBlockItemSprite(@Nullable TextureAtlasSprite srcSprite) {
        if (srcSprite == null) return null;

        TextureAtlasSprite cached = itemToBlock.get(srcSprite);
        if (cached != null) {
            return isMissingSprite(cached) ? null : cached;
        }

        TextureAtlas blockAtlas = getBlockAtlas();
        Identifier srcId = srcSprite.contents().name();

        TextureAtlasSprite result =
                trySprite(blockAtlas, srcId);

        if (result == null) {
            String ns = srcId.getNamespace();
            String path = srcId.getPath();
            String name = extractItemName(path);

            result = trySprite(blockAtlas, Identifier.tryParse(ns + ":item/" + name));
            if (result == null) {
                result = trySprite(blockAtlas, Identifier.tryParse("minecraft:item/" + ns + "/" + name));
            }
        }

        itemToBlock.put(srcSprite, result != null ? result : getMissingSprite());
        return result;
    }

    public @Nullable TextureAtlasSprite tryResolveEntitySprite(@Nullable Identifier textureId) {
        if (textureId == null) return null;

        Identifier spriteId = normalizeTexturePathToSpriteId(textureId);
        if (spriteId == null) return null;

        return trySprite(getBlockAtlas(), spriteId);
    }

    private static TextureAtlas getBlockAtlas() {
        return Minecraft.getInstance()
                .getAtlasManager()
                .getAtlasOrThrow(AtlasIds.BLOCKS);
    }

    private static TextureAtlasSprite getMissingSprite() {
        return getBlockAtlas().getSprite(MISSINGNO);
    }

    private static boolean isMissingSprite(TextureAtlasSprite sprite) {
        TextureAtlasSprite missing = getMissingSprite();
        return sprite == missing || sprite.contents() == missing.contents();
    }

    private static @Nullable TextureAtlasSprite trySprite(TextureAtlas atlas, @Nullable Identifier id) {
        if (id == null) return null;

        TextureAtlasSprite sprite = atlas.getSprite(id);
        return isMissingSprite(sprite) ? null : sprite;
    }

    private static String extractItemName(String path) {
        if (path.startsWith("item/")) {
            return path.substring("item/".length());
        }

        int idx = path.indexOf("item/");
        if (idx >= 0) {
            return path.substring(idx + "item/".length());
        }

        return path;
    }

    private static @Nullable Identifier normalizeTexturePathToSpriteId(Identifier textureId) {
        String ns = textureId.getNamespace();
        String path = textureId.getPath();

        if (path.startsWith("textures/") && path.endsWith(".png")) {
            String core = path.substring("textures/".length(), path.length() - ".png".length());
            return Identifier.tryParse(ns + ":" + core);
        }

        if (path.endsWith(".png")) {
            String core = path.substring(0, path.length() - ".png".length());
            return Identifier.tryParse(ns + ":" + core);
        }

        return textureId;
    }
}
