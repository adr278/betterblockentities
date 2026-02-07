package betterblockentities.client.chunk.pipeline.shelf;

/* minecraft */
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;

/* annotations */
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

/* java */
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

    public boolean isNotBlockAtlas(@Nullable TextureAtlasSprite sprite) {
        return sprite == null || sprite.atlasLocation() != AtlasIds.BLOCKS;
    }

    public @Nullable TextureAtlasSprite tryGetBlockItemSprite(@Nullable TextureAtlasSprite srcSprite) {
        if (srcSprite == null) return null;

        TextureAtlasSprite cached = itemToBlock.get(srcSprite);
        if (cached != null) return isMissingSprite(cached) ? null : cached;

        Identifier srcId = srcSprite.contents().name();
        TextureAtlasSprite result = findBlockItemSprite(srcId);

        itemToBlock.put(srcSprite, result != null ? result : getMissingSprite());
        return result;
    }

    public @Nullable TextureAtlasSprite tryResolveEntitySprite(@Nullable Identifier textureId) {
        return trySprite(getBlockAtlas(), normalizeTexturePathToSpriteId(textureId));
    }

    private static @Nullable TextureAtlasSprite findBlockItemSprite(Identifier srcId) {
        TextureAtlas atlas = getBlockAtlas();

        TextureAtlasSprite sprite = trySprite(atlas, srcId);
        if (sprite != null) return sprite;

        String ns = srcId.getNamespace();
        String name = extractItemName(srcId.getPath());

        sprite = trySprite(atlas, Identifier.tryParse(ns + ":item/" + name));
        return sprite != null ? sprite : trySprite(atlas, Identifier.tryParse("minecraft:item/" + ns + "/" + name));
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
        if (path.startsWith("item/")) return path.substring(5);

        int idx = path.indexOf("item/");
        return idx >= 0 ? path.substring(idx + 5) : path;
    }

    private static @Nullable Identifier normalizeTexturePathToSpriteId(@Nullable Identifier textureId) {
        if (textureId == null) return null;

        String path = textureId.getPath();
        if (!path.endsWith(".png")) return textureId;

        String core = path.startsWith("textures/")
                ? path.substring(9, path.length() - 4)
                : path.substring(0, path.length() - 4);
        return Identifier.tryParse(textureId.getNamespace() + ":" + core);
    }
}
