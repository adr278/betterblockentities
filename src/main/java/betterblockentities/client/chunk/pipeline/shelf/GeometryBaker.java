package betterblockentities.client.chunk.pipeline.shelf;

/* minecraft */
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ShelfBlock;
import net.minecraft.world.level.block.entity.ShelfBlockEntity;
import net.minecraft.world.phys.AABB;

public final class GeometryBaker {
    @FunctionalInterface public interface Sink {
        void accept(PackedQuad quad, Object renderTypeObj, int[] tintLayers);
    }

    public record PackedQuad(
            // positions.
            float x0, float y0, float z0,
            float x1, float y1, float z1,
            float x2, float y2, float z2,
            float x3, float y3, float z3,
            // packed UVs.
            long uv0, long uv1, long uv2, long uv3,
            // metadata.
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
            BlockModelPart part
    ) {}

    private final RenderTypeClassifier rt;
    private final PackedQuadUtil quadUtil;
    private final SubmitNodeGeometryReader submitReader;
    private final LayeredGeometryAssembler assembler;

    public GeometryBaker(RenderTypeClassifier rt, SpriteRemapper sprites) {
        this.rt = rt;
        this.quadUtil = new PackedQuadUtil(sprites);
        this.submitReader = new SubmitNodeGeometryReader(rt, quadUtil);
        this.assembler = new LayeredGeometryAssembler();
    }

    public LayeredPart[] bakeLayeredParts(
            Level level,
            ShelfBlockEntity shelf,
            int slot,
            ItemStack stack,
            int light,
            CacheKeys.StackKey sk,
            boolean skipGlintGeometry
    ) {
        LayeredGeometryAssembler.Run run = assembler.newRun();

        captureShelfItemGeometry(level, shelf, slot, stack, light, sk, (pq, renderTypeObj, tintLayers) -> {
            PackedQuad baked = quadUtil.normalizeForCaching(pq);
            RenderTypeClassifier.Info info = rt.info(renderTypeObj, baked.sprite());
            if (skipGlintGeometry && info.glint()) return;
            int resolvedTint = CacheKeys.NO_TINT;
            int tintIndex = baked.tintIndex();
            if (tintIndex >= 0 && tintLayers != null && tintIndex < tintLayers.length) {
                resolvedTint = tintLayers[tintIndex];
            }
            assembler.accept(run, baked, info.layer(), resolvedTint);
        });
        return assembler.finish(run);
    }

    public void captureShelfItemGeometry(
            Level level,
            ShelfBlockEntity shelf,
            int slot,
            ItemStack stack,
            int light,
            CacheKeys.StackKey sk,
            Sink sink
    ) {
        Minecraft mc = Minecraft.getInstance();
        ItemModelResolver resolver = mc.getItemModelResolver();

        ItemStackRenderState state = new ItemStackRenderState();
        SubmitNodeStorage storage = new SubmitNodeStorage();
        PoseStack pose = new PoseStack();

        int seedBase = CacheKeys.stableSeed(sk);

        resolver.updateForTopItem(
                state,
                stack,
                ItemDisplayContext.ON_SHELF,
                level,
                shelf,
                seedBase + slot
        );

        if (state.isEmpty()) {
            return;
        }

        PackedQuadUtil.resetPoseToIdentity(pose);

        Direction facing = shelf.getBlockState().getValue(ShelfBlock.FACING);
        float rotY = facing.getAxis().isHorizontal() ? -facing.toYRot() : 180.0F;

        float x = (float) (slot - 1) * 0.3125F;
        double y = shelf.getAlignItemsToBottom() ? -0.25D : 0.0D;
        double z = -0.25D;

        pose.pushPose();
        pose.translate(0.5F, 0.5F, 0.5F);
        pose.mulPose(Axis.YP.rotationDegrees(rotY));
        pose.translate(x, y, z);
        pose.scale(0.25F, 0.25F, 0.25F);

        AABB aabb = state.getModelBoundingBox();
        double dy = -aabb.minY;
        if (!shelf.getAlignItemsToBottom()) {
            dy += -(aabb.maxY - aabb.minY) / 2.0D;
        }
        pose.translate(0.0D, dy, 0.0D);

        state.submit(pose, storage, light, OverlayTexture.NO_OVERLAY, 0);

        for (SubmitNodeCollection coll : storage.getSubmitsPerOrder().values()) {
            submitReader.read(coll, sink);
        }

        pose.popPose();
    }
}
