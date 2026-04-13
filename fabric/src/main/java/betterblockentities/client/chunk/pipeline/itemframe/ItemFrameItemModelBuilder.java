package betterblockentities.client.chunk.pipeline.itemframe;

/* local */
import betterblockentities.client.chunk.pipeline.shelf.CacheKeys;
import betterblockentities.client.chunk.pipeline.shelf.GeometryBaker;
import betterblockentities.client.chunk.pipeline.shelf.RenderTypeClassifier;
import betterblockentities.client.chunk.pipeline.shelf.SpriteRemapper;

/* minecraft */
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/* java */
import java.util.ArrayList;

public final class ItemFrameItemModelBuilder {
    public record LayeredQuad(
            ChunkSectionLayer layer,
            int color,
            GeometryBaker.PackedQuad quad
    ) {}

    public record CapturedMesh(LayeredQuad[] quads) {
        public static final CapturedMesh EMPTY = new CapturedMesh(new LayeredQuad[0]);

        public boolean isEmpty() { return this.quads.length == 0; }
    }

    public record CaptureResult(CapturedMesh mesh, ItemFrameContentRenderMode contentRenderMode) {
        public static final CaptureResult EMPTY = new CaptureResult(CapturedMesh.EMPTY, ItemFrameContentRenderMode.NONE);
    }

    private static final RenderTypeClassifier RT = new RenderTypeClassifier();
    private static final SpriteRemapper SPRITES = new SpriteRemapper();
    private static final GeometryBaker BAKER = new GeometryBaker(RT, SPRITES);

    private static final ThreadLocal<ItemStackRenderState> RENDER_STATES = ThreadLocal.withInitial(ItemStackRenderState::new);

    public static CaptureResult captureMesh(ItemFrame frame, ItemStack stack) {
        if (stack.isEmpty()) return CaptureResult.EMPTY;
        return bake(frame, stack);
    }

    private static CaptureResult bake(ItemFrame frame, ItemStack stack) {
        Minecraft minecraft = Minecraft.getInstance();
        ItemModelResolver resolver = minecraft.getItemModelResolver();
        ItemStackRenderState state = RENDER_STATES.get();

        state.clear();
        resolver.updateForNonLiving(state, stack, ItemDisplayContext.FIXED, frame);

        if (state.isEmpty()) return new CaptureResult(CapturedMesh.EMPTY, ItemFrameContentRenderMode.IMMEDIATE_ITEM);

        ArrayList<LayeredQuad> quads = new ArrayList<>(32);

        boolean supported = BAKER.captureItemGeometryFromResolvedState(
                state,
                (packedQuad, renderTypeObj, tintLayers) -> {
                    GeometryBaker.PackedQuad baked = BAKER.normalizeForCaching(packedQuad);
                    RenderTypeClassifier.Info info = RT.info(renderTypeObj);

                    int resolvedTint = CacheKeys.NO_TINT;
                    int tintIndex = baked.tintIndex();
                    if (tintIndex >= 0 && tintLayers != null && tintIndex < tintLayers.length) {
                        resolvedTint = tintLayers[tintIndex];
                    }

                    quads.add(new LayeredQuad(info.layer(), resolvedTint, baked));
                },
                true
        );

        if (!supported || quads.isEmpty()) return new CaptureResult(CapturedMesh.EMPTY, ItemFrameContentRenderMode.IMMEDIATE_ITEM);

        return new CaptureResult(new CapturedMesh(quads.toArray(new LayeredQuad[0])), ItemFrameContentRenderMode.NONE);
    }

    public static void invalidateResolverStateOnReload() {
        SPRITES.clear();
        RT.clear();
    }
}
