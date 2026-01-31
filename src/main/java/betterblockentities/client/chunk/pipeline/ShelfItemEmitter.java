package betterblockentities.client.chunk.pipeline;

import betterblockentities.client.itemmesh.ItemMeshCapture;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.caffeinemc.mods.sodium.client.render.model.MutableQuadViewImpl;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.ShelfBlockEntity;

import java.util.Locale;

public final class ShelfItemEmitter {
    private ShelfItemEmitter() {}

    private static final float VANILLA_ITEM_SCALE = 0.50f;
    private static final float VANILLA_BOTTOM_OFFSET = -0.25f;

    private static final float BASE_Y = 0.50f;
    private static final float BASE_Z = 0.7502f;

    public static void emit(MutableQuadViewImpl emitter, BlockPos pos, Level level, ShelfBlockEntity shelf) {
        final int light = LevelRenderer.getLightColor(level, pos);

        for (int i = 0; i < 3; i++) {
            ItemStack stack = shelf.getItems().get(i);
            if (stack.isEmpty()) continue;

            final float pixel = 1.0f / 16.0f;

            final float xBase = 0.75f - (i * 0.25f);
            // i=0 right, i=1 middle, i=2 left

            final float x =
            (i == 0) ? (xBase + pixel) :
            (i == 2) ? (xBase - pixel) :
            xBase;

            PoseStack pose = new PoseStack();

            // Rotate around block center based on shelf facing
            pose.translate(0.5, 0.5, 0.5);
            pose.mulPose(Axis.YP.rotationDegrees(shelf.getVisualRotationYInDegrees()));
            pose.translate(-0.5, -0.5, -0.5);

            // Move to slot position
            pose.translate(x, BASE_Y, BASE_Z);

            // Vanilla scale
            pose.scale(VANILLA_ITEM_SCALE, VANILLA_ITEM_SCALE, VANILLA_ITEM_SCALE);

            // Vanilla align to bottom: item-space offset applied after scale
            if (shelf.getAlignItemsToBottom()) {
                pose.translate(0.0, VANILLA_BOTTOM_OFFSET, 0.0);
            }

            int seed = (int) (pos.asLong() ^ (i * 31L));

            ItemMeshCapture.capture(
                    stack,
                    ItemDisplayContext.FIXED,
                    level,
                    pose,
                    light,
                    0,
                    0,
                    seed,
                    (BakedQuad quad, Object renderTypeObj, int[] tintLayers) -> {
                        emitter.fromBakedQuad(quad);
                        emitter.setCullFace(null);

                        emitter.setRenderType(mapLayer(renderTypeObj));

                        int ti = quad.tintIndex();
                        if (ti >= 0 && tintLayers != null && ti < tintLayers.length) {
                            int tint = tintLayers[ti];
                            for (int v = 0; v < 4; v++) emitter.setColor(v, tint);
                        }

                        emitter.emitDirectly();
                        emitter.clear();
                    }
            );
        }
    }

    private static ChunkSectionLayer mapLayer(Object renderTypeObj) {
        if (renderTypeObj == null) return ChunkSectionLayer.CUTOUT;

        // Prefer real type checks when possible
        if (renderTypeObj instanceof RenderType rt) {
            String n = rt.toString().toLowerCase(Locale.ROOT);
            if (n.contains("translucent")) return ChunkSectionLayer.TRANSLUCENT;
            if (n.contains("solid")) return ChunkSectionLayer.SOLID;
            return ChunkSectionLayer.CUTOUT;
        }

        String s = renderTypeObj.toString().toLowerCase(Locale.ROOT);
        if (s.contains("translucent")) return ChunkSectionLayer.TRANSLUCENT;
        if (s.contains("solid")) return ChunkSectionLayer.SOLID;
        return ChunkSectionLayer.CUTOUT;
    }
}
