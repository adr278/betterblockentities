package betterblockentities.client.itemmesh;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.List;

/**
 * Capture item baked-quads via the SubmitNode pipeline.
 * No accessors/mixins needed.
**/
public final class ItemMeshCapture {
    private ItemMeshCapture() {}

    @FunctionalInterface
    public interface Sink {
        void accept(BakedQuad quad, Object renderTypeObj, int[] tintLayers);
    }

    private static final ThreadLocal<ItemStackRenderState> TL_STATE =
            ThreadLocal.withInitial(ItemStackRenderState::new);

    private static final ThreadLocal<SubmitNodeStorage> TL_STORAGE =
            ThreadLocal.withInitial(SubmitNodeStorage::new);

    public static void capture(
            ItemStack stack,
            ItemDisplayContext displayContext,
            @Nullable Level levelOrNull,
            PoseStack basePose,
            int packedLight,
            int packedOverlay,
            int outlineColor,
            int seed,
            Sink sink
    ) {
        if (stack == null || stack.isEmpty()) return;

        Minecraft mc = Minecraft.getInstance();
        Level level = (levelOrNull != null) ? levelOrNull : mc.level;
        if (level == null) return;

        ItemModelResolver resolver = mc.getItemModelResolver();

        ItemStackRenderState state = TL_STATE.get();
        SubmitNodeStorage storage = TL_STORAGE.get();

        state.clear();
        storage.clear();

        // Build the render state for a placed item
        resolver.updateForTopItem(state, stack, displayContext, level, null, seed);

        if (state.isEmpty()) return;

        // Emit submits into storage
        state.submit(basePose, storage, packedLight, packedOverlay, outlineColor);

        // Read back baked quad submits
        for (SubmitNodeCollection coll : storage.getSubmitsPerOrder().values()) {
            for (SubmitNodeStorage.ItemSubmit submit : coll.getItemSubmits()) {
                Matrix4f mat = submit.pose().pose();
                int[] tints = submit.tintLayers();
                Object rtObj = submit.renderType();

                List<BakedQuad> quads = submit.quads();
                for (BakedQuad quad : quads) {
                    sink.accept(transformQuadPositions(quad, mat), rtObj, tints);
                }
            }
        }
    }

    private static BakedQuad transformQuadPositions(BakedQuad q, Matrix4f m) {
        Vector3f p0 = new Vector3f(q.position0());
        Vector3f p1 = new Vector3f(q.position1());
        Vector3f p2 = new Vector3f(q.position2());
        Vector3f p3 = new Vector3f(q.position3());

        m.transformPosition(p0);
        m.transformPosition(p1);
        m.transformPosition(p2);
        m.transformPosition(p3);

        return new BakedQuad(
                p0, p1, p2, p3,
                q.packedUV0(), q.packedUV1(), q.packedUV2(), q.packedUV3(),
                q.tintIndex(),
                q.direction(),
                q.sprite(),
                q.shade(),
                q.lightEmission()
        );
    }
}
