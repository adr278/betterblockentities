package betterblockentities.util;

/* sodium */


/* fabric */
import net.caffeinemc.mods.sodium.client.render.model.MutableQuadViewImpl;

/* minecraft */
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;

public class ModelTransform {
    /* rotates base MutableQuadView from passed degrees and not radians */
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

    /* swaps the sprite of the quad while preserving UV mapping proportions (no caching) recompute for every quad */
    public static void swapSprite(TextureAtlasSprite newSprite, MutableQuadViewImpl quad) {
        if (!(quad instanceof MutableQuadViewImpl mQuad)) return;
        TextureAtlasSprite oldSprite = mQuad.cachedSprite();
        if (oldSprite == null) return;

        for (int i = 0; i < 4; i++) {
            float uNorm = (mQuad.getTexU(i) - oldSprite.getU0()) / (oldSprite.getU1() - oldSprite.getU0());
            float vNorm = (mQuad.getTexV(i) - oldSprite.getV0()) / (oldSprite.getV1() - oldSprite.getV0());
            mQuad.setUV(i,
                        newSprite.getU0() + uNorm * (newSprite.getU1() - newSprite.getU0()),
                        newSprite.getV0() + vNorm * (newSprite.getV1() - newSprite.getV0())
                );
            }
            mQuad.cachedSprite(newSprite);
    }

    /* swaps the sprite of the quad while preserving UV mapping proportions (caching) */
    public static void swapSpriteCached(TextureAtlasSprite newSprite, MutableQuadViewImpl quad) {
        final float uNewMin = newSprite.getU0();
        final float uNewMax = newSprite.getU1();
        final float vNewMin = newSprite.getV0();
        final float vNewMax = newSprite.getV1();

        if (!(quad instanceof MutableQuadViewImpl mQuad)) return;
        TextureAtlasSprite old = mQuad.cachedSprite();
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
            float uNorm = (mQuad.getTexU(i) - uOldMin) / uOldRange;
            float vNorm = (mQuad.getTexV(i) - vOldMin) / vOldRange;

            mQuad.setUV(i,
                    uNewMin + uNorm * uNewRange,
                    vNewMin + vNorm * vNewRange
            );
        }
        mQuad.cachedSprite(newSprite);
    }

    /* gets an existing sprite from the atlas */
    public static TextureAtlasSprite getSprite(Identifier id) {
        var atlas = Minecraft.getInstance()
                .getAtlasManager()
                .getAtlasOrThrow(AtlasIds.BLOCKS);
        return atlas.getSprite(id);
    }

    private ModelTransform() {
        throw new IllegalStateException("Instancing of this class is not allowed!");
    }
}
