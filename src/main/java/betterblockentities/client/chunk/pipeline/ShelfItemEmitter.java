package betterblockentities.client.chunk.pipeline;

import betterblockentities.client.itemmesh.ItemMeshCapture;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.caffeinemc.mods.sodium.client.render.model.MutableQuadViewImpl;
import net.minecraft.client.renderer.LevelRenderer;
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

    public static void emit(MutableQuadViewImpl emitter, BlockPos pos, Level level, ShelfBlockEntity shelf) {
        int light = LevelRenderer.getLightColor(level, pos);

        for (int i = 0; i < 3; i++) {
            ItemStack stack = shelf.getItems().get(i);
            if (stack.isEmpty()) continue;

            float x = (i == 0) ? 0.25f : (i == 1) ? 0.50f : 0.75f;
            float y = shelf.getAlignItemsToBottom() ? 0.22f : 0.35f;
            float z = 0.88f;
            float s = 0.35f;

            PoseStack pose = new PoseStack();

            pose.translate(0.5, 0.5, 0.5);
            pose.mulPose(Axis.YP.rotationDegrees(shelf.getVisualRotationYInDegrees()));
            pose.translate(-0.5, -0.5, -0.5);

            pose.translate(x, y, z);
            pose.scale(s, s, s);

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

        String s = renderTypeObj.toString().toLowerCase(Locale.ROOT);
        if (s.contains("translucent")) return ChunkSectionLayer.TRANSLUCENT;
        if (s.contains("solid")) return ChunkSectionLayer.SOLID;
        return ChunkSectionLayer.CUTOUT;
    }
}
