package betterblockentities.client.chunk.util;

/* minecraft */
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;

/* sodium */
import net.caffeinemc.mods.sodium.client.render.model.MutableQuadViewImpl;

/* java/misc */
import org.joml.Vector3f;

/**
 * Utility class for transforming Sodium's {@link net.caffeinemc.mods.sodium.client.render.model.MutableQuadViewImpl } emitter / render data
 */
public final class QuadTransform {
    private QuadTransform() {
        throw new IllegalStateException("Instancing of this class is not allowed!");
    }

    public static void rotateY(MutableQuadViewImpl quad, float degrees) {
        float radians = (float) Math.toRadians(degrees);
        float cos = (float) Math.cos(radians);
        float sin = (float) Math.sin(radians);
        float centerX = 0.5f;
        float centerZ = 0.5f;

        for (int i = 0; i < 4; i++) {
            float x = quad.getX(i) - centerX;
            float y = quad.getY(i);
            float z = quad.getZ(i) - centerZ;
            float newX = x * cos - z * sin;
            float newZ = x * sin + z * cos;
            quad.setPos(i, newX + centerX, y, newZ + centerZ);
        }
    }

    public static void rotateX(MutableQuadViewImpl quad, float degrees) {
        float radians = (float) Math.toRadians(degrees);
        float cos = (float) Math.cos(radians);
        float sin = (float) Math.sin(radians);

        float centerY = 0.5f;
        float centerZ = 0.5f;

        for (int i = 0; i < 4; i++) {
            float x = quad.getX(i);
            float y = quad.getY(i) - centerY;
            float z = quad.getZ(i) - centerZ;

            float newY = y * cos - z * sin;
            float newZ = y * sin + z * cos;

            quad.setPos(i, x, newY + centerY, newZ + centerZ);
        }
    }

    public static void pushAndExpand(float amount, MutableQuadViewImpl quad) {
        /* compute quad center */
        float cx = 0, cy = 0, cz = 0;
        for (int i = 0; i < 4; i++) {
            cx += quad.getX(i);
            cy += quad.getY(i);
            cz += quad.getZ(i);
        }
        cx *= 0.25f;
        cy *= 0.25f;
        cz *= 0.25f;

        Vector3f normal = new Vector3f(quad.faceNormal()).normalize();

        /* compute tangent */
        Vector3f tangent = new Vector3f(
                quad.getX(1) - quad.getX(0),
                quad.getY(1) - quad.getY(0),
                quad.getZ(1) - quad.getZ(0)
        ).normalize();

        Vector3f bitangent = new Vector3f(normal).cross(tangent).normalize();

        /* scalar for diagonal expansion */
        float expand = amount;

        /* move each vertex */
        for (int i = 0; i < 4; i++) {
            float x = quad.getX(i);
            float y = quad.getY(i);
            float z = quad.getZ(i);

            /* local coordinates relative to center */
            float dx = x - cx;
            float dy = y - cy;
            float dz = z - cz;

            /* project vertex into tangent space */
            float tx = dx * tangent.x() + dy * tangent.y() + dz * tangent.z();
            float ty = dx * bitangent.x() + dy * bitangent.y() + dz * bitangent.z();

            /* move outward in 2D plane (diagonally) */
            x += tangent.x() * Math.signum(tx) * expand;
            y += tangent.y() * Math.signum(tx) * expand;
            z += tangent.z() * Math.signum(tx) * expand;

            x += bitangent.x() * Math.signum(ty) * expand;
            y += bitangent.y() * Math.signum(ty) * expand;
            z += bitangent.z() * Math.signum(ty) * expand;

            /* push outward along face normal */
            x += normal.x() * amount;
            y += normal.y() * amount;
            z += normal.z() * amount;

            quad.setPos(i, x, y, z);
        }
    }

    public static void swapSprite(TextureAtlasSprite newSprite, MutableQuadViewImpl quad) {
        final float uNewMin = newSprite.getU0();
        final float uNewMax = newSprite.getU1();
        final float vNewMin = newSprite.getV0();
        final float vNewMax = newSprite.getV1();

        TextureAtlasSprite old = quad.cachedSprite();
        if (old == null) return;

        float uOldMin = old.getU0();
        float uOldMax = old.getU1();
        float vOldMin = old.getV0();
        float vOldMax = old.getV1();

        float uOldRange = uOldMax - uOldMin;
        float vOldRange = vOldMax - vOldMin;

        float uNewRange = uNewMax - uNewMin;
        float vNewRange = vNewMax - vNewMin;

        for (int i = 0; i < 4; i++) {
            float uNorm = (quad.getTexU(i) - uOldMin) / uOldRange;
            float vNorm = (quad.getTexV(i) - vOldMin) / vOldRange;

            quad.setUV(i,
                    uNewMin + uNorm * uNewRange,
                    vNewMin + vNorm * vNewRange
            );
        }

        quad.cachedSprite(newSprite);
    }

    public static TextureAtlasSprite getSprite(Identifier spriteId) {
        return getBlockSprite(spriteId);
    }

    public static TextureAtlasSprite getSprite(Identifier atlasId, Identifier spriteId) {
        TextureAtlas atlas = Minecraft.getInstance()
                .getAtlasManager()
                .getAtlasOrThrow(atlasId);
        return atlas.getSprite(spriteId);
    }

    public static TextureAtlasSprite getBlockSprite(Identifier spriteId) {
        return getSprite(AtlasIds.BLOCKS, spriteId);
    }

    public static TextureAtlasSprite getItemSprite(Identifier spriteId) {
        return getSprite(AtlasIds.ITEMS, spriteId);
    }

    public static Identifier stitchedId(Identifier original) {
        Identifier id = Identifier.tryParse("minecraft:item/" + original.getNamespace() + "/" + original.getPath());
        return (id != null) ? id : original;
    }

    public static TextureAtlasSprite getStitchedItemSprite(Identifier originalItemId) {
        if (!originalItemId.getPath().startsWith("item/")) return null;
        Identifier stitched = stitchedId(originalItemId);
        return getItemSprite(stitched);
    }
}
