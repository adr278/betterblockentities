package betterblockentities.client.chunk.pipeline.shelf;

/* local */
import betterblockentities.mixin.render.immediate.blockentity.shelf.RenderSetupAccessor;
import betterblockentities.mixin.render.immediate.blockentity.shelf.RenderTypeAccessor;
import betterblockentities.mixin.render.immediate.blockentity.shelf.TextureBindingAccessor;

/* minecraft */
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.Identifier;

/* annotations */
import org.jetbrains.annotations.Nullable;

/* java */
import java.util.Locale;
import java.util.Map;

public final class RenderTypeClassifier {
    public record Info(ChunkSectionLayer layer, boolean glint) {}
    public record SpriteAwareRenderType(@Nullable TextureAtlasSprite sprite, String name) {}

    private static final Identifier TEX_MISS = Identifier.tryParse("bbe:__rt_tex_miss");

    private static final Info INFO_NULL = new Info(ChunkSectionLayer.CUTOUT, false);

    private static final Info[][] SHARED = {
            {new Info(ChunkSectionLayer.SOLID, false), new Info(ChunkSectionLayer.SOLID, true)},
            {new Info(ChunkSectionLayer.CUTOUT, false), new Info(ChunkSectionLayer.CUTOUT, true)},
            {new Info(ChunkSectionLayer.TRANSLUCENT, false), new Info(ChunkSectionLayer.TRANSLUCENT, true)}
    };

    public void clear() {}

    public Info info(Object renderTypeObj) {
        if (renderTypeObj == null) return INFO_NULL;
        return classify(renderTypeObj);
    }

    public @Nullable Identifier tryExtractTextureId(RenderType rt) {
        if (rt == null) return null;

        Identifier id;
        try {
            Object setup = ((RenderTypeAccessor) rt).GetState();
            Map<String, Object> textures = ((RenderSetupAccessor) setup).GetTexture();

            if (textures == null || textures.isEmpty()) return null;

            Object binding = textures.values().iterator().next();
            Identifier location = ((TextureBindingAccessor) binding).GetLocation();
            id = location != null ? location : TEX_MISS;
        } catch (Throwable ignored) {
            id = TEX_MISS;
        }

        return id == TEX_MISS ? null : id;
    }

    private Info classify(Object obj) {
        if (obj instanceof ChunkSectionLayer layer) {
            return shared(layer, false);
        }

        if (obj instanceof SpriteAwareRenderType(TextureAtlasSprite sprite, String name1)) {
            boolean glint = isGlint(name1);
            Info fromSprite = infoFromSprite(sprite, glint);
            return fromSprite != null ? fromSprite : shared(fallbackLayer(name1), glint);
        }

        String name = String.valueOf(obj);
        return shared(fallbackLayer(name), isGlint(name));
    }

    private static @Nullable Info infoFromSprite(@Nullable TextureAtlasSprite sprite, boolean glint) {
        if (sprite == null) return null;

        try {
            var transparency = sprite.contents().transparency();

            if (transparency.hasTranslucent()) return shared(ChunkSectionLayer.TRANSLUCENT, glint);

            if (transparency.hasTransparent()) return shared(ChunkSectionLayer.CUTOUT, glint);

            return shared(ChunkSectionLayer.SOLID, glint);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static ChunkSectionLayer fallbackLayer(String name) {
        String lower = name.toLowerCase(Locale.ROOT);

        if (lower.contains("translucent")) return ChunkSectionLayer.TRANSLUCENT;

        if (lower.contains("solid")) return ChunkSectionLayer.SOLID;

        return ChunkSectionLayer.CUTOUT;
    }

    private static boolean isGlint(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        return lower.contains("glint") || lower.contains("foil");
    }

    private static Info shared(ChunkSectionLayer layer, boolean glint) {
        return SHARED[index(layer)][glint ? 1 : 0];
    }

    private static int index(ChunkSectionLayer layer) {
        return switch (layer) {
            case SOLID -> 0;
            case CUTOUT -> 1;
            case TRANSLUCENT -> 2;
        };
    }
}
