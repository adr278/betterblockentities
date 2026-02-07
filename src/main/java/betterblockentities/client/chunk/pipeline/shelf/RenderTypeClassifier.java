package betterblockentities.client.chunk.pipeline.shelf;

/* local */
import betterblockentities.mixin.render.immediate.blockentity.shelf.RenderSetupAccessor;
import betterblockentities.mixin.render.immediate.blockentity.shelf.RenderTypeAccessor;
import betterblockentities.mixin.render.immediate.blockentity.shelf.TextureBindingAccessor;

/* minecraft */
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;

/* mixin */
import org.jetbrains.annotations.Nullable;

/* java/misc */
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public final class RenderTypeClassifier {
    public record Info(ChunkSectionLayer layer, boolean glint) {}

    private static final Identifier TEX_MISS =
            Identifier.tryParse("bbe:__rt_tex_miss");

    private final ConcurrentHashMap<Object, Info> infoCache =
            new ConcurrentHashMap<>();

    private final ConcurrentHashMap<RenderType, Identifier> texCache =
            new ConcurrentHashMap<>();

    private static final Info INFO_NULL =
            new Info(ChunkSectionLayer.CUTOUT, false);

    private static final Info INFO_SOLID =
            new Info(ChunkSectionLayer.SOLID, false);

    private static final Info INFO_CUTOUT =
            new Info(ChunkSectionLayer.CUTOUT, false);

    private static final Info INFO_TRANSLUCENT =
            new Info(ChunkSectionLayer.TRANSLUCENT, false);

    private static final Info INFO_SOLID_GLINT =
            new Info(ChunkSectionLayer.SOLID, true);

    private static final Info INFO_CUTOUT_GLINT =
            new Info(ChunkSectionLayer.CUTOUT, true);

    private static final Info INFO_TRANSLUCENT_GLINT =
            new Info(ChunkSectionLayer.TRANSLUCENT, true);

    public void clear() {
        infoCache.clear();
        texCache.clear();
    }

    public Info info(Object renderTypeObj) {
        if (renderTypeObj == null) return INFO_NULL;

        // Single map op on hits/misses.
        return infoCache.computeIfAbsent(renderTypeObj, obj -> {
            // Fallback to name heuristic.
            String s = (obj instanceof RenderType rt) ? rt.toString() : obj.toString();

            final boolean glint = containsIgnoreCase(s, "glint") || containsIgnoreCase(s, "foil");

            final ChunkSectionLayer layer;
            if (containsIgnoreCase(s, "translucent")) layer = ChunkSectionLayer.TRANSLUCENT;
            else if (containsIgnoreCase(s, "solid")) layer = ChunkSectionLayer.SOLID;
            else layer = ChunkSectionLayer.CUTOUT;

            // Return shared constants.
            return switch (layer) {
                case SOLID -> glint ? INFO_SOLID_GLINT : INFO_SOLID;
                case TRANSLUCENT -> glint ? INFO_TRANSLUCENT_GLINT : INFO_TRANSLUCENT;
                default -> glint ? INFO_CUTOUT_GLINT : INFO_CUTOUT;
            };
        });
    }

    public @Nullable @SuppressWarnings("ConstantConditions") Identifier tryExtractTextureId(RenderType rt) {
        if (rt == null) return null;

        Identifier out = texCache.computeIfAbsent(rt, key -> {
            try {
                Object setup = ((RenderTypeAccessor) key).GetState();

                // RenderSetup is final; accessor interface is injected at runtime by mixin.
                RenderSetupAccessor rsa = (RenderSetupAccessor) setup;

                Map<String, Object> textures = rsa.GetTexture();
                if (textures == null || textures.isEmpty()) {
                    return TEX_MISS;
                }

                Object binding = textures.values().iterator().next();
                TextureBindingAccessor tba = (TextureBindingAccessor) binding;

                Identifier id = tba.GetLocation();
                return id != null ? id : TEX_MISS;
            } catch (Throwable t) {
                return TEX_MISS;
            }
        });

        return (out == TEX_MISS) ? null : out;
    }

    private static boolean containsIgnoreCase(String haystack, String needle) {
        final int hLen = haystack.length();
        final int nLen = needle.length();
        if (nLen == 0) return true;
        if (nLen > hLen) return false;

        // Cheap pre-check using regionMatches.
        for (int i = 0; i <= hLen - nLen; i++) {
            if (haystack.regionMatches(true, i, needle, 0, nLen)) {
                return true;
            }
        }
        return false;
    }
}