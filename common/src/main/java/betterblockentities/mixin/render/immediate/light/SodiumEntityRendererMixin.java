package betterblockentities.mixin.render.immediate.light;

/* local */
import betterblockentities.client.render.immediate.light.ImmediateBlockEntityLight;

/* sodium */
import net.caffeinemc.mods.sodium.api.util.ColorARGB;
import net.caffeinemc.mods.sodium.api.util.NormI8;
import net.caffeinemc.mods.sodium.client.render.immediate.model.EntityRenderer;
import net.caffeinemc.mods.sodium.client.render.immediate.model.ModelCuboid;
import net.caffeinemc.mods.sodium.client.util.Int2;

/* mixin */
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/* java/misc */
import java.nio.ByteOrder;

@Mixin(EntityRenderer.class)
public class SodiumEntityRendererMixin {
    @Unique private static final int BBE$FACE_COUNT = 6;
    @Unique private static final int BBE$FACE_VERTICES = 4;
    // Sodium has already baked terrain directional shading into the vertex color.
    @Unique private static final int BBE$LIGHTING_NORMAL = NormI8.pack(0F, 1F, 0F);
    @Unique private static final boolean BBE$LITTLE_ENDIAN = ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN;

    @Shadow private static long[] CUBE_VERTEX_ZW;

    @Shadow
    private static long writeVertex(long pointer, int positionIndex, long texture, long overlayLight, int normal) {
        throw new AssertionError();
    }

    @Inject(method = "emitQuads", at = @At("HEAD"), cancellable = true)
    private static void bbe$emitImmediateLitQuads(
            long pointer,
            ModelCuboid cuboid,
            int overlay,
            int fallbackLight,
            CallbackInfoReturnable<Integer> cir
    ) {
        ImmediateBlockEntityLight.RenderContext context = ImmediateBlockEntityLight.active();
        if (context == null) {
            return;
        }

        long currentPointer = pointer;
        int vertexCount = 0;

        for (int face = 0; face < BBE$FACE_COUNT; face++) {
            if (!cuboid.shouldDrawFace(face)) {
                continue;
            }

            int vertexBase = face * BBE$FACE_VERTICES;
            int position0 = cuboid.positions[vertexBase];
            int position1 = cuboid.positions[vertexBase + 1];
            int position2 = cuboid.positions[vertexBase + 2];
            int position3 = cuboid.positions[vertexBase + 3];
            context.prepareLocalQuadLight(
                    bbe$localX(cuboid, position0), bbe$localY(cuboid, position0), bbe$localZ(cuboid, position0),
                    bbe$localX(cuboid, position1), bbe$localY(cuboid, position1), bbe$localZ(cuboid, position1),
                    bbe$localX(cuboid, position2), bbe$localY(cuboid, position2), bbe$localZ(cuboid, position2),
                    bbe$localX(cuboid, position3), bbe$localY(cuboid, position3), bbe$localZ(cuboid, position3)
            );

            currentPointer = bbe$writeLitVertex(context, currentPointer, position0, cuboid.textures[vertexBase], overlay, 0);
            currentPointer = bbe$writeLitVertex(context, currentPointer, position1, cuboid.textures[vertexBase + 1], overlay, 1);
            currentPointer = bbe$writeLitVertex(context, currentPointer, position2, cuboid.textures[vertexBase + 2], overlay, 2);
            currentPointer = bbe$writeLitVertex(context, currentPointer, position3, cuboid.textures[vertexBase + 3], overlay, 3);
            vertexCount += BBE$FACE_VERTICES;
        }

        cir.setReturnValue(vertexCount);
    }

    @Unique
    private static long bbe$writeLitVertex(
            ImmediateBlockEntityLight.RenderContext context,
            long pointer,
            int positionIndex,
            long texture,
            int overlay,
            int vertexIndex
    ) {
        long zw = CUBE_VERTEX_ZW[positionIndex];
        int color = ColorARGB.fromABGR(bbe$secondInt(zw));
        ImmediateBlockEntityLight.VertexLight vertexLight = context.applyPreparedVertex(
                vertexIndex,
                color
        );
        long litZw = bbe$withSecondInt(zw, ColorARGB.toABGR(vertexLight.color()));
        long overlayLight = Int2.pack(overlay, vertexLight.light());

        CUBE_VERTEX_ZW[positionIndex] = litZw;
        long nextPointer = writeVertex(pointer, positionIndex, texture, overlayLight, BBE$LIGHTING_NORMAL);
        CUBE_VERTEX_ZW[positionIndex] = zw;

        return nextPointer;
    }

    @Unique
    private static float bbe$localX(ModelCuboid cuboid, int positionIndex) {
        return switch (positionIndex) {
            case 1, 2, 5, 6 -> cuboid.originX + cuboid.sizeX;
            default -> cuboid.originX;
        };
    }

    @Unique
    private static float bbe$localY(ModelCuboid cuboid, int positionIndex) {
        return switch (positionIndex) {
            case 2, 3, 6, 7 -> cuboid.originY + cuboid.sizeY;
            default -> cuboid.originY;
        };
    }

    @Unique
    private static float bbe$localZ(ModelCuboid cuboid, int positionIndex) {
        return positionIndex >= 4 ? cuboid.originZ + cuboid.sizeZ : cuboid.originZ;
    }

    @Unique
    private static int bbe$secondInt(long value) {
        return BBE$LITTLE_ENDIAN ? (int)(value >>> 32) : (int)value;
    }

    @Unique
    private static long bbe$withSecondInt(long value, int second) {
        if (BBE$LITTLE_ENDIAN) {
            return (value & 0xFFFFFFFFL) | ((long)second << 32);
        }

        return (value & 0xFFFFFFFF00000000L) | ((long)second & 0xFFFFFFFFL);
    }
}
