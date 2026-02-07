package betterblockentities.client.chunk.pipeline.shelf;

/* minecraft */
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
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

    public static final int VARIANT_COUNT = 24;

    public static int variantIndex(int slot, Direction facing, boolean alignBottom) {
        int slotIndex = switch (slot) {
            case 0, 1, 2 -> slot;
            default -> throw new IllegalArgumentException("slot must be 0..2");
        };

        int facingIndex = switch (facing) {
            case EAST  -> 1;
            case SOUTH -> 2;
            case WEST  -> 3;
            default -> 0;
        };

        int alignOffset = alignBottom ? 12 : 0;
        return alignOffset + (facingIndex * 3) + slotIndex;
    }

    public static BlockModelPart[] transformParts(
            GeometryBaker.LayeredPart[] parts,
            GeometryBaker.CanonicalBounds bounds,
            int slot,
            Direction facing,
            boolean alignBottom
    ) {
        BlockModelPart[] out = new BlockModelPart[parts.length];
        for (int i = 0; i < parts.length; i++) {
            out[i] = transformPart(parts[i].part(), bounds, slot, facing, alignBottom);
        }
        return out;
    }

    private static BlockModelPart transformPart(
            BlockModelPart source,
            GeometryBaker.CanonicalBounds bounds,
            int slot,
            Direction facing,
            boolean alignBottom
    ) {
        float slotX = (slot - 1) * 0.3125F;
        float localY = alignBottom ? -bounds.minY() : -((bounds.minY() + bounds.maxY()) * 0.5F);

        Rotation rotation = rotationForFacing(facing);
        List<BakedQuad>[] quadsByFace = buildQuadCache(source, slotX, localY, rotation.cos, rotation.sin);

        return new CachedBlockModelPart(
                quadsByFace,
                source.particleIcon(),
                source.useAmbientOcclusion()
        );
    }

    private static List<BakedQuad>[] buildQuadCache(
            BlockModelPart source,
            float slotX,
            float localY,
            float cos,
            float sin
    ) {
        @SuppressWarnings("unchecked") List<BakedQuad>[] out = (List<BakedQuad>[]) new List[7];

        out[0] = transformQuadList(source.getQuads(null), slotX, localY, cos, sin);

        for (Direction direction : Direction.values()) {
            out[faceIndex(direction)] = transformQuadList(source.getQuads(direction), slotX, localY, cos, sin);
        }
        return out;
    }

    private static List<BakedQuad> transformQuadList(
            List<BakedQuad> sourceQuads,
            float slotX,
            float localY,
            float cos,
            float sin
    ) {
        if (sourceQuads.isEmpty()) {
            return Collections.emptyList();
        }

        ArrayList<BakedQuad> out = new ArrayList<>(sourceQuads.size());
        for (BakedQuad sourceQuad : sourceQuads) {
            out.add(transformQuad(sourceQuad, slotX, localY, cos, sin));
        }
        return Collections.unmodifiableList(out);
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

        Direction direction = deriveDirection(p0, p1, p2, p3);
        if (direction == null) {
            direction = quad.direction();
        }

        return new BakedQuad(
                p0, p1, p2, p3,
                quad.packedUV0(), quad.packedUV1(), quad.packedUV2(), quad.packedUV3(),
                quad.tintIndex(),
                direction,
                quad.sprite(),
                quad.shade(),
                quad.lightEmission()
        );
    }

    private static Vector3f transformVertex(
            Vector3fc pos,
            float slotX,
            float localY,
            float cos,
            float sin
    ) {
        float x = pos.x() + slotX;
        float y = pos.y() + localY;
        float z = pos.z() - 0.25F;

        float rx = x * cos - z * sin;
        float rz = x * sin + z * cos;

        return new Vector3f(rx + 0.5F, y + 0.5F, rz + 0.5F);
    }

    private static @Nullable Direction deriveDirection(
            Vector3fc p0,
            Vector3fc p1,
            Vector3fc p2,
            Vector3fc p3
    ) {
        float ax = p1.x() - p0.x();
        float ay = p1.y() - p0.y();
        float az = p1.z() - p0.z();

        float bx = p2.x() - p0.x();
        float by = p2.y() - p0.y();
        float bz = p2.z() - p0.z();

        float nx = ay * bz - az * by;
        float ny = az * bx - ax * bz;
        float nz = ax * by - ay * bx;

        float len2 = nx * nx + ny * ny + nz * nz;
        if (len2 < 1.0e-8F) {
            ax = p2.x() - p1.x();
            ay = p2.y() - p1.y();
            az = p2.z() - p1.z();

            bx = p3.x() - p1.x();
            by = p3.y() - p1.y();
            bz = p3.z() - p1.z();

            nx = ay * bz - az * by;
            ny = az * bx - ax * bz;
            nz = ax * by - ay * bx;

            len2 = nx * nx + ny * ny + nz * nz;
            if (len2 < 1.0e-8F) {
                return null;
            }
        }

        return Direction.getApproximateNearest(nx, ny, nz);
    }

    private static int faceIndex(@Nullable Direction direction) {
        return direction == null ? 0 : direction.ordinal() + 1;
    }

    private static Rotation rotationForFacing(Direction facing) {
        return switch (facing) {
            case SOUTH -> Rotation.SOUTH;
            case WEST  -> Rotation.WEST;
            case EAST  -> Rotation.EAST;
            default    -> Rotation.NORTH;
        };
    }

    private record Rotation(float cos, float sin) {
        private static final Rotation SOUTH = new Rotation(1.0F, 0.0F);
        private static final Rotation WEST  = new Rotation(0.0F, 1.0F);
        private static final Rotation NORTH = new Rotation(-1.0F, 0.0F);
        private static final Rotation EAST  = new Rotation(0.0F, -1.0F);
    }

    private record CachedBlockModelPart(List<BakedQuad>[] quadsByFace, TextureAtlasSprite particleIcon,
                                        boolean useAmbientOcclusion) implements BlockModelPart {

        @Override public @NonNull List<BakedQuad> getQuads(@Nullable Direction direction) {
                return quadsByFace[faceIndex(direction)];
            }
        }
}
