package betterblockentities.client.model.geometry;

/* minecraft */
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.core.Direction;

/* mojang */
import com.mojang.blaze3d.vertex.PoseStack;

/* java/misc */
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Stores pre-baked model-part geometry templates used by the Sodium terrain BE pipeline.
 */
public final class GeometryRegistry {
    private static final ConcurrentHashMap<ModelLayerLocation, ModelTemplate> CACHE = new ConcurrentHashMap<>();

    private GeometryRegistry() {
    }

    public static void cacheGeometry(final ModelLayerLocation key, final ModelPart root, final PoseStack stack) {
        CACHE.put(key, ModelUtility.bakeTemplate(root, stack));
    }

    public static ModelTemplate getModel(final ModelLayerLocation layer) {
        return CACHE.get(layer);
    }

    public static void clearCache() {
        CACHE.clear();
    }

    public static Map<ModelLayerLocation, ModelTemplate> getCache() {
        return CACHE;
    }

    public static final class ModelTemplate {
        private final List<QuadTemplate> quads;
        private final Map<String, List<QuadTemplate>> partQuads;
        private final Map<Direction, List<QuadTemplate>> quadsByFace;

        public ModelTemplate(final List<QuadTemplate> quads, final Map<String, List<QuadTemplate>> partQuads) {
            this.quads = List.copyOf(quads);

            final Map<String, List<QuadTemplate>> partsCopy = new HashMap<>();
            for (Map.Entry<String, List<QuadTemplate>> entry : partQuads.entrySet()) {
                partsCopy.put(entry.getKey(), List.copyOf(entry.getValue()));
            }
            this.partQuads = Collections.unmodifiableMap(partsCopy);

            final Map<Direction, List<QuadTemplate>> byFace = new EnumMap<>(Direction.class);
            for (Direction direction : Direction.values()) {
                byFace.put(direction, new ArrayList<>());
            }
            for (QuadTemplate quad : quads) {
                byFace.get(quad.face()).add(quad);
            }

            final Map<Direction, List<QuadTemplate>> immutableByFace = new EnumMap<>(Direction.class);
            for (Direction direction : Direction.values()) {
                immutableByFace.put(direction, Collections.unmodifiableList(byFace.get(direction)));
            }
            this.quadsByFace = Collections.unmodifiableMap(immutableByFace);
        }

        public List<QuadTemplate> quads() {
            return this.quads;
        }

        public List<QuadTemplate> quads(final String part) {
            return this.partQuads.getOrDefault(part, List.of());
        }

        public List<QuadTemplate> quads(final Direction face) {
            return this.quadsByFace.getOrDefault(face, List.of());
        }
    }

    public record QuadTemplate(
            float[] positions,
            float[] uvs,
            float[] normals,
            Direction face
    ) {
    }
}
