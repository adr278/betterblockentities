package betterblockentities.client.chunk.pipeline.shelf;

/* minecraft */
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.Direction;

/* java */
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/* joml */
import org.joml.Vector3f;
import org.joml.Vector3fc;

/* annotations */
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

public final class ShelfPartTransformer {
    private ShelfPartTransformer() {}
    private static final List<List<BakedQuad>> EMPTY_DIRECTIONAL_BUCKETS = createEmptyDirectionalBuckets();

    private static List<List<BakedQuad>> createEmptyDirectionalBuckets() {
        List<List<BakedQuad>> buckets = new ArrayList<>(7);
        for (int i = 0; i < 7; i++) {
            buckets.add(Collections.emptyList());
        }
        return buckets;
    }

    public static BlockStateModelPart[] transformParts(
            GeometryBaker.LayeredPart[] parts,
            GeometryBaker.CanonicalBounds bounds,
            int slot,
            Direction facing,
            boolean alignBottom
    ) {
        BlockStateModelPart[] out = new BlockStateModelPart[parts.length];

        float slotX = (slot - 1) * 0.3125F;
        float localY = alignBottom ? -bounds.minY() : -((bounds.minY() + bounds.maxY()) * 0.5F);

        float cos = switch (facing) {
            case SOUTH -> 1.0F;
            case WEST, EAST -> 0.0F;
            default -> -1.0F;
        };

        float sin = switch (facing) {
            case WEST -> 1.0F;
            case EAST -> -1.0F;
            default -> 0.0F;
        };

        for (int i = 0; i < parts.length; i++) {
            BlockStateModelPart source = parts[i].part();
            out[i] = new CachedBlockStateModelPart(
                    buildQuadCache(source, slotX, localY, cos, sin),
                    source.particleMaterial(),
                    source.materialFlags(),
                    source.useAmbientOcclusion()
            );
        }
        return out;
    }

    private static List<List<BakedQuad>> buildQuadCache(
            BlockStateModelPart source,
            float slotX,
            float localY,
            float cos,
            float sin
    ) {
        List<BakedQuad> unculled = transformQuads(source.getQuads(null), slotX, localY, cos, sin);
        if (unculled.isEmpty()) return EMPTY_DIRECTIONAL_BUCKETS;

        List<List<BakedQuad>> out = new ArrayList<>(EMPTY_DIRECTIONAL_BUCKETS);
        out.set(0, unculled);
        return out;
    }

    private static List<BakedQuad> transformQuads(
            List<BakedQuad> sourceQuads,
            float slotX,
            float localY,
            float cos,
            float sin
    ) {
        if (sourceQuads.isEmpty()) return Collections.emptyList();

        List<BakedQuad> out = new ArrayList<>(sourceQuads.size());
        for (BakedQuad quad : sourceQuads)
            out.add(transformQuad(quad, slotX, localY, cos, sin));
        return out;
    }

    private static BakedQuad transformQuad(
            BakedQuad quad,
            float slotX,
            float localY,
            float cos,
            float sin
    ) {
        Vector3f p0 = transformVertex(quad.position0(), slotX, localY, cos, sin);
        Vector3f p1 = transformVertex(quad.position1(), slotX, localY, cos, sin);
        Vector3f p2 = transformVertex(quad.position2(), slotX, localY, cos, sin);
        Vector3f p3 = transformVertex(quad.position3(), slotX, localY, cos, sin);

        Direction direction = rotateDirection(quad.direction(), cos, sin);
        BakedQuad.MaterialInfo info = quad.materialInfo();

        return new BakedQuad(
                p0, p1, p2, p3,
                quad.packedUV0(), quad.packedUV1(), quad.packedUV2(), quad.packedUV3(),
                direction,
                new BakedQuad.MaterialInfo(
                        info.sprite(),
                        info.layer(),
                        info.itemRenderType(),
                        info.tintIndex(),
                        info.shade(),
                        info.lightEmission()
                )
        );
    }

    private static Vector3f transformVertex(Vector3fc pos, float slotX, float localY, float cos, float sin) {
        float x = pos.x() + slotX;
        float y = pos.y() + localY;
        float z = pos.z() - 0.25F;

        return new Vector3f(
                x * cos - z * sin + 0.5F,
                y + 0.5F,
                x * sin + z * cos + 0.5F
        );
    }

    private static Direction rotateDirection(Direction direction, float cos, float sin) {
        return switch (direction) {
            case NORTH -> horizontalFromVector(sin, -cos);
            case SOUTH -> horizontalFromVector(-sin, cos);
            case EAST -> horizontalFromVector(cos, sin);
            case WEST -> horizontalFromVector(-cos, -sin);
            case UP, DOWN -> direction;
        };
    }

    private static Direction horizontalFromVector(float x, float z) {
        if (z > 0.5F) return Direction.SOUTH;
        if (z < -0.5F) return Direction.NORTH;
        if (x > 0.5F) return Direction.EAST;
        if (x < -0.5F) return Direction.WEST;
        return Direction.NORTH;
    }

    private record CachedBlockStateModelPart(
            List<List<BakedQuad>> quadsByFace,
            Material.Baked particleMaterial,
            int materialFlags,
            boolean useAmbientOcclusion
    ) implements BlockStateModelPart {

        @Override public @NonNull List<BakedQuad> getQuads(@Nullable Direction direction) {
            return quadsByFace.get(direction == null ? 0 : direction.ordinal() + 1);
        }

        @Override public Material.@NonNull Baked particleMaterial() {
            return particleMaterial;
        }

        @Override public int materialFlags() {
            return materialFlags;
        }

        @Override public boolean useAmbientOcclusion() {
            return useAmbientOcclusion;
        }
    }
}
