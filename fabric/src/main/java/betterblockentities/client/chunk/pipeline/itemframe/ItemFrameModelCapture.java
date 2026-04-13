package betterblockentities.client.chunk.pipeline.itemframe;

/* local */
import betterblockentities.client.chunk.pipeline.shelf.GeometryBaker;
import betterblockentities.client.chunk.pipeline.shelf.SpriteRemapper;

/* minecraft */
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;

/* joml */
import org.joml.Matrix4f;
import org.joml.Vector3f;

/* java */
import java.util.ArrayList;
import java.util.List;

public final class ItemFrameModelCapture {
    private static final SpriteRemapper SPRITES = new SpriteRemapper();

    private static final Identifier WOOD_TEXTURE = Identifier.withDefaultNamespace("block/birch_planks");
    private static final Identifier ITEM_FRAME_BACK_TEXTURE = Identifier.withDefaultNamespace("block/item_frame");
    private static final Identifier GLOW_ITEM_FRAME_BACK_TEXTURE = Identifier.withDefaultNamespace("block/glow_item_frame");

    private static final float FULL_DEPTH = 16.0F;
    private static final float WOOD_EDGE_MIN = 15.0F;
    private static final float WOOD_EDGE_MAX = 16.0F;

    private static final FrameSpec NORMAL_FRAME = new FrameSpec(
            2.0F,
            14.0F,
            3.0F,
            13.0F,
            15.0F,
            15.5F
    );
    private record FrameSpec(
            float outerMin,
            float outerMax,
            float openingMin,
            float openingMax,
            float rimMinZ,
            float backMinZ
    ) {}

    public static ItemFrameItemModelBuilder.CapturedMesh getFrameMesh(boolean glow) { return buildFrameMesh(glow); }

    private static ItemFrameItemModelBuilder.CapturedMesh buildFrameMesh(boolean glow) {
        TextureAtlasSprite woodSprite = SPRITES.tryResolveEntitySprite(WOOD_TEXTURE);
        TextureAtlasSprite backSprite = SPRITES.tryResolveEntitySprite(
                glow ? GLOW_ITEM_FRAME_BACK_TEXTURE : ITEM_FRAME_BACK_TEXTURE
        );

        if (woodSprite == null || backSprite == null) return null;

        ArrayList<ItemFrameItemModelBuilder.LayeredQuad> out = new ArrayList<>(32);
        Matrix4f pose = new Matrix4f().translation(-0.5F, -0.5F, -0.5F);

        emitFrame(NORMAL_FRAME, pose, woodSprite, backSprite, out);

        if (out.isEmpty()) return null;

        return new ItemFrameItemModelBuilder.CapturedMesh(out.toArray(new ItemFrameItemModelBuilder.LayeredQuad[0]));
    }

    private static void emitFrame(
            FrameSpec spec,
            Matrix4f pose,
            TextureAtlasSprite woodSprite,
            TextureAtlasSprite backSprite,
            List<ItemFrameItemModelBuilder.LayeredQuad> out
    ) {
        emitBackPlate(spec, pose, backSprite, out);
        emitHorizontalRim(spec.outerMin(), spec.outerMax(), spec.outerMin(), spec.openingMin(), spec.rimMinZ(), pose, woodSprite, out);
        emitHorizontalRim(spec.outerMin(), spec.outerMax(), spec.openingMax(), spec.outerMax(), spec.rimMinZ(), pose, woodSprite, out);
        emitVerticalRim(spec.outerMin(), spec.openingMin(), spec.openingMin(), spec.openingMax(), spec.rimMinZ(), pose, woodSprite, out);
        emitVerticalRim(spec.openingMax(), spec.outerMax(), spec.openingMin(), spec.openingMax(), spec.rimMinZ(), pose, woodSprite, out);
    }

    private static void emitBackPlate(
            FrameSpec spec,
            Matrix4f pose,
            TextureAtlasSprite sprite,
            List<ItemFrameItemModelBuilder.LayeredQuad> out
    ) {
        addFace(out, pose, sprite, Direction.NORTH,
                spec.openingMin(), spec.openingMin(), spec.backMinZ(),
                spec.openingMax(), spec.openingMax(),
                spec.openingMin(), spec.openingMin(), spec.openingMax(), spec.openingMax());
        addFace(out, pose, sprite, Direction.SOUTH,
                spec.openingMin(), spec.openingMin(), spec.backMinZ(),
                spec.openingMax(), spec.openingMax(),
                spec.openingMin(), spec.openingMin(), spec.openingMax(), spec.openingMax());
    }

    private static void emitHorizontalRim(
            float minX,
            float maxX,
            float minY,
            float maxY,
            float minZ,
            Matrix4f pose,
            TextureAtlasSprite sprite,
            List<ItemFrameItemModelBuilder.LayeredQuad> out
    ) {
        float sideV0 = FULL_DEPTH - maxY;
        float sideV1 = FULL_DEPTH - minY;

        addFace(out, pose, sprite, Direction.DOWN, minX, minY, minZ, maxX, maxY, minX, 0.0F, maxX, 1.0F);
        addFace(out, pose, sprite, Direction.UP, minX, minY, minZ, maxX, maxY, minX, 15.0F, maxX, 16.0F);
        addFace(out, pose, sprite, Direction.NORTH, minX, minY, minZ, maxX, maxY, FULL_DEPTH - maxX, sideV0, FULL_DEPTH - minX, sideV1);
        addFace(out, pose, sprite, Direction.SOUTH, minX, minY, minZ, maxX, maxY, minX, sideV0, maxX, sideV1);
        addFace(out, pose, sprite, Direction.WEST, minX, minY, minZ, maxX, maxY, WOOD_EDGE_MIN, sideV0, WOOD_EDGE_MAX, sideV1);
        addFace(out, pose, sprite, Direction.EAST, minX, minY, minZ, maxX, maxY, 0.0F, sideV0, 1.0F, sideV1);
    }

    private static void emitVerticalRim(
            float minX,
            float maxX,
            float minY,
            float maxY,
            float minZ,
            Matrix4f pose,
            TextureAtlasSprite sprite,
            List<ItemFrameItemModelBuilder.LayeredQuad> out
    ) {
        addFace(out, pose, sprite, Direction.NORTH, minX, minY, minZ, maxX, maxY, FULL_DEPTH - maxX, minY, FULL_DEPTH - minX, maxY);
        addFace(out, pose, sprite, Direction.SOUTH, minX, minY, minZ, maxX, maxY, minX, minY, maxX, maxY);
        addFace(out, pose, sprite, Direction.WEST, minX, minY, minZ, maxX, maxY, WOOD_EDGE_MIN, minY, WOOD_EDGE_MAX, maxY);
        addFace(out, pose, sprite, Direction.EAST, minX, minY, minZ, maxX, maxY, 0.0F, minY, 1.0F, maxY);
    }

    private static void addFace(
            List<ItemFrameItemModelBuilder.LayeredQuad> out,
            Matrix4f pose,
            TextureAtlasSprite sprite,
            Direction direction,
            float minX,
            float minY,
            float minZ,
            float maxX,
            float maxY,
            float u0,
            float v0,
            float u1,
            float v1
    ) {
        out.add(new ItemFrameItemModelBuilder.LayeredQuad(
                ChunkSectionLayer.SOLID,
                -1,
                buildQuad(pose, sprite, direction, minX, minY, minZ, maxX, maxY, u0, v0, u1, v1)
        ));
    }

    private static GeometryBaker.PackedQuad buildQuad(
            Matrix4f pose,
            TextureAtlasSprite sprite,
            Direction direction,
            float minX,
            float minY,
            float minZ,
            float maxX,
            float maxY,
            float u0,
            float v0,
            float u1,
            float v1
    ) {
        Vector3f[] positions = faceVertices(direction, minX, minY, minZ, maxX, maxY);
        for (Vector3f position : positions) pose.transformPosition(position);

        return new GeometryBaker.PackedQuad(
                positions[0].x, positions[0].y, positions[0].z,
                positions[1].x, positions[1].y, positions[1].z,
                positions[2].x, positions[2].y, positions[2].z,
                positions[3].x, positions[3].y, positions[3].z,
                packUv(sprite, u0, v0),
                packUv(sprite, u0, v1),
                packUv(sprite, u1, v1),
                packUv(sprite, u1, v0),
                direction,
                true,
                0,
                -1,
                sprite,
                sprite
        );
    }

    private static Vector3f[] faceVertices(
            Direction direction,
            float minX,
            float minY,
            float minZ,
            float maxX,
            float maxY
    ) {
        float x0 = minX / 16.0F;
        float y0 = minY / 16.0F;
        float z0 = minZ / 16.0F;
        float x1 = maxX / 16.0F;
        float y1 = maxY / 16.0F;
        float z1 = ItemFrameModelCapture.FULL_DEPTH / 16.0F;

        return switch (direction) {
            case NORTH -> new Vector3f[]{
                    new Vector3f(x1, y1, z0),
                    new Vector3f(x1, y0, z0),
                    new Vector3f(x0, y0, z0),
                    new Vector3f(x0, y1, z0)
            };
            case SOUTH -> new Vector3f[]{
                    new Vector3f(x0, y1, z1),
                    new Vector3f(x0, y0, z1),
                    new Vector3f(x1, y0, z1),
                    new Vector3f(x1, y1, z1)
            };
            case WEST -> new Vector3f[]{
                    new Vector3f(x0, y1, z0),
                    new Vector3f(x0, y0, z0),
                    new Vector3f(x0, y0, z1),
                    new Vector3f(x0, y1, z1)
            };
            case EAST -> new Vector3f[]{
                    new Vector3f(x1, y1, z1),
                    new Vector3f(x1, y0, z1),
                    new Vector3f(x1, y0, z0),
                    new Vector3f(x1, y1, z0)
            };
            case UP -> new Vector3f[]{
                    new Vector3f(x0, y1, z0),
                    new Vector3f(x0, y1, z1),
                    new Vector3f(x1, y1, z1),
                    new Vector3f(x1, y1, z0)
            };
            case DOWN -> new Vector3f[]{
                    new Vector3f(x0, y0, z1),
                    new Vector3f(x0, y0, z0),
                    new Vector3f(x1, y0, z0),
                    new Vector3f(x1, y0, z1)
            };
        };
    }

    private static long packUv(TextureAtlasSprite sprite, float u, float v) {
        float packedU = sprite.getU0() + (sprite.getU1() - sprite.getU0()) * (u / 16.0F);
        float packedV = sprite.getV0() + (sprite.getV1() - sprite.getV0()) * (v / 16.0F);
        return UVPair.pack(packedU, packedV);
    }

    public static void invalidateAllCachesOnReload() { SPRITES.clear(); }
}
