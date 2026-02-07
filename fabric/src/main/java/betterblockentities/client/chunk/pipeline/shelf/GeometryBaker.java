package betterblockentities.client.chunk.pipeline.shelf;

/* local */
import betterblockentities.client.render.immediate.blockentity.renderers.BBEShelfItemRenderer;

/* mojang */
import com.mojang.blaze3d.vertex.PoseStack;

/* minecraft */
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/* annotations */
import org.jspecify.annotations.NonNull;

public final class GeometryBaker {
    @FunctionalInterface public interface Sink {
        void accept(PackedQuad quad, Object renderTypeObj, int[] tintLayers);
    }

    public record PackedQuad(
            float x0, float y0, float z0,
            float x1, float y1, float z1,
            float x2, float y2, float z2,
            float x3, float y3, float z3,
            long uv0, long uv1, long uv2, long uv3,
            Direction dir,
            boolean shade,
            int lightEmission,
            int tintIndex,
            TextureAtlasSprite sprite,
            TextureAtlasSprite spriteForCacheOrNull
    ) {}

    public record LayeredPart(
            ChunkSectionLayer layer,
            int color,
            BlockStateModelPart part
    ) {}

    public record CanonicalBounds(
            float minX, float maxX,
            float minY, float maxY,
            float minZ, float maxZ
    ) {}

    public record CanonicalMesh(LayeredPart[] parts, CanonicalBounds bounds) {}

    private final RenderTypeClassifier rt;
    private final PackedQuadUtil quadUtil;
    private final LayeredGeometryAssembler assembler;

    private final ThreadLocal<ItemStackRenderState> renderStates;
    private final ThreadLocal<PoseStack> capturePoses;
    private final ThreadLocal<LayeredGeometryAssembler.Run> assemblerRuns;
    private final ThreadLocal<BBEShelfItemRenderer> collectors;

    public GeometryBaker(RenderTypeClassifier rt, SpriteRemapper sprites) {
        this.rt = rt;
        this.quadUtil = new PackedQuadUtil(sprites);
        this.assembler = new LayeredGeometryAssembler();

        this.renderStates = ThreadLocal.withInitial(ItemStackRenderState::new);
        this.capturePoses = ThreadLocal.withInitial(PoseStack::new);
        this.assemblerRuns = ThreadLocal.withInitial(this.assembler::newRun);
        this.collectors = ThreadLocal.withInitial(() -> new BBEShelfItemRenderer(this.rt, this.quadUtil));
    }

    public CanonicalMesh bakeCanonicalMesh(
            Level level,
            ItemStack stack,
            CacheKeys.StackKey sk,
            boolean skipGlintGeometry
    ) {
        Minecraft mc = Minecraft.getInstance();
        ItemModelResolver resolver = mc.getItemModelResolver();
        ItemStackRenderState state = renderStates.get();

        int seedBase = CacheKeys.stableSeed(sk);

        resolver.updateForTopItem(
                state,
                stack,
                ItemDisplayContext.ON_SHELF,
                level,
                null,
                seedBase
        );

        if (state.isEmpty()) {
            return new CanonicalMesh(
                    new LayeredPart[0],
                    new CanonicalBounds(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F)
            );
        }

        AABB aabb = state.getModelBoundingBox();
        CanonicalBounds bounds = getBounds(aabb);

        LayeredGeometryAssembler.Run run = assemblerRuns.get();
        run.reset();

        captureCanonicalItemGeometryFromResolvedState(state, (pq, renderTypeObj, tintLayers) -> {
            PackedQuad baked = quadUtil.normalizeForCaching(pq);

            RenderTypeClassifier.Info info = rt.info(renderTypeObj);
            if (skipGlintGeometry && info.glint()) return;

            int resolvedTint = CacheKeys.NO_TINT;
            int tintIndex = baked.tintIndex();
            if (tintIndex >= 0 && tintLayers != null && tintIndex < tintLayers.length) {
                resolvedTint = tintLayers[tintIndex];
            }

            assembler.accept(run, baked, info.layer(), resolvedTint);
        });

        return new CanonicalMesh(assembler.finish(run), bounds);
    }

    private static @NonNull CanonicalBounds getBounds(AABB aabb) {
        final float scale = 0.25F;
        return new CanonicalBounds(
                (float) (aabb.minX * scale), (float) (aabb.maxX * scale),
                (float) (aabb.minY * scale), (float) (aabb.maxY * scale),
                (float) (aabb.minZ * scale), (float) (aabb.maxZ * scale)
        );
    }

    public void captureCanonicalItemGeometryFromResolvedState(
            ItemStackRenderState state,
            Sink sink
    ) {
        PoseStack pose = capturePoses.get();
        PackedQuadUtil.resetPoseToIdentity(pose);

        pose.pushPose();
        pose.scale(0.25F, 0.25F, 0.25F);

        BBEShelfItemRenderer collector = collectors.get();
        collector.reset(sink);

        try {
            state.submit(pose, collector, 0, OverlayTexture.NO_OVERLAY, 0);
        } finally {
            pose.popPose();
            collector.reset(null);
        }
    }
}
