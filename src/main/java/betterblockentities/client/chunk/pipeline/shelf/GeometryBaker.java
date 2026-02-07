package betterblockentities.client.chunk.pipeline.shelf;

/* local */
import betterblockentities.client.model.geometry.RecordingVertexConsumer;
import betterblockentities.mixin.render.immediate.blockentity.shelf.CustomFeatureRendererStorageAccessor;
import betterblockentities.mixin.render.immediate.blockentity.shelf.ModelFeatureRendererStorageAccessor;
import betterblockentities.mixin.render.immediate.blockentity.shelf.ModelPartFeatureRendererStorageAccessor;

/* minecraft */
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.feature.CustomFeatureRenderer;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.feature.ModelPartFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ShelfBlock;
import net.minecraft.world.level.block.entity.ShelfBlockEntity;
import net.minecraft.world.phys.AABB;

/* java/misc */
import org.joml.Matrix4f;
import org.joml.Vector3f;
import java.util.ArrayList;
import java.util.List;

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

    public record CachedQuad(
            PackedQuad quad,
            ChunkSectionLayer layer,
            int tint
    ) {}

    public record CachedMesh(CachedQuad[] quads) {
        public boolean isEmpty() { return quads.length == 0; }
    }

    private final RenderTypeClassifier rt;
    private final SpriteRemapper sprites;

    // Threadlocals.
    private final ThreadLocal<ItemStackRenderState> tlState = ThreadLocal.withInitial(ItemStackRenderState::new);
    private final ThreadLocal<SubmitNodeStorage> tlStorage = ThreadLocal.withInitial(SubmitNodeStorage::new);
    private final ThreadLocal<RecordingVertexConsumer> tlRec = ThreadLocal.withInitial(RecordingVertexConsumer::new);
    private final ThreadLocal<PoseStack> tlCapturePose = ThreadLocal.withInitial(PoseStack::new);
    private final ThreadLocal<PoseStack> tlTessPose = ThreadLocal.withInitial(PoseStack::new);
    private final ThreadLocal<int[]> tlOneTint = ThreadLocal.withInitial(() -> new int[1]);
    private final ThreadLocal<ArrayList<CachedQuad>> tlSolid = ThreadLocal.withInitial(() -> new ArrayList<>(64));
    private final ThreadLocal<ArrayList<CachedQuad>> tlCutout = ThreadLocal.withInitial(() -> new ArrayList<>(64));
    private final ThreadLocal<ArrayList<CachedQuad>> tlTranslucent = ThreadLocal.withInitial(() -> new ArrayList<>(64));
    private static final ThreadLocal<V4> TL_V4 = ThreadLocal.withInitial(V4::new);

    private static final class V4 {
        final Vector3f a = new Vector3f();
        final Vector3f b = new Vector3f();
        final Vector3f c = new Vector3f();
        final Vector3f d = new Vector3f();
    }

    public GeometryBaker(RenderTypeClassifier rt, SpriteRemapper sprites) {
        this.rt = rt;
        this.sprites = sprites;
    }

    /**
     * Capture and tessellate item submits, returning a baked mesh bucketed by {@link ChunkSectionLayer}.
     *
     * @param skipGlintGeometry if true, glint render types are ignored during baking.
     */
    public CachedMesh bakeMesh(Level level, ShelfBlockEntity shelf, int slot, ItemStack stack, int light, CacheKeys.StackKey sk, boolean skipGlintGeometry) {
        ArrayList<CachedQuad> solid = tlSolid.get();
        ArrayList<CachedQuad> cutout = tlCutout.get();
        ArrayList<CachedQuad> translucent = tlTranslucent.get();

        solid.clear();
        cutout.clear();
        translucent.clear();

        captureShelfItemGeometry(level, shelf, slot, stack, light, sk, (pq, renderTypeObj, tintLayers) -> {
            RenderTypeClassifier.Info info = rt.info(renderTypeObj);
            if (skipGlintGeometry && info.glint()) return;

            ChunkSectionLayer layer = info.layer();
            PackedQuad baked = normalizeSpriteForCaching(pq);

            int tint = CacheKeys.NO_TINT;
            int tintIndex = baked.tintIndex();
            if (tintIndex >= 0 && tintLayers != null && tintIndex < tintLayers.length) {
                tint = tintLayers[tintIndex];
            }

            CachedQuad cq = new CachedQuad(baked, layer, tint);

            // Bucket by layer.
            switch (layer) {
                case SOLID -> solid.add(cq);
                case CUTOUT -> cutout.add(cq);
                default -> translucent.add(cq);
            }
        });

        int size = solid.size() + cutout.size() + translucent.size();
        if (size == 0) {
            solid.clear();
            cutout.clear();
            translucent.clear();
            return new CachedMesh(new CachedQuad[0]);
        }

        CachedQuad[] arr = new CachedQuad[size];
        int i = 0;
        for (CachedQuad q : solid) arr[i++] = q;
        for (CachedQuad q : cutout) arr[i++] = q;
        for (CachedQuad q : translucent) arr[i++] = q;

        solid.clear();
        cutout.clear();
        translucent.clear();

        return new CachedMesh(arr);
    }

    /* ---------------- Geometry capture ---------------- */

    public void captureShelfItemGeometry(Level level, ShelfBlockEntity shelf, int slot, ItemStack stack, int light, CacheKeys.StackKey sk, Sink sink) {
        Minecraft mc = Minecraft.getInstance();
        ItemModelResolver resolver = mc.getItemModelResolver();

        ItemStackRenderState state = tlState.get();
        state.clear();

        int seedBase = CacheKeys.stableSeed(sk);

        resolver.updateForTopItem(
                state,
                stack,
                ItemDisplayContext.ON_SHELF,
                level,
                shelf,
                seedBase + slot
        );

        if (state.isEmpty()) return;

        PoseStack pose = tlCapturePose.get();
        resetPoseToIdentity(pose);

        Direction facing = shelf.getBlockState().getValue(ShelfBlock.FACING);
        float rotY = facing.getAxis().isHorizontal() ? -facing.toYRot() : 180.0F;

        float g = (float) (slot - 1) * 0.3125F;
        double offY = shelf.getAlignItemsToBottom() ? -0.25D : 0.0D;
        double offZ = -0.25D;

        pose.pushPose();
        pose.translate(0.5F, 0.5F, 0.5F);
        pose.mulPose(Axis.YP.rotationDegrees(rotY));
        pose.translate(g, offY, offZ);
        pose.scale(0.25F, 0.25F, 0.25F);

        AABB aabb = state.getModelBoundingBox();
        double d = -aabb.minY;
        if (!shelf.getAlignItemsToBottom()) {
            d += -(aabb.maxY - aabb.minY) / 2.0D;
        }
        pose.translate(0.0D, d, 0.0D);

        SubmitNodeStorage storage = tlStorage.get();
        storage.clear();

        state.submit(pose, storage, light, OverlayTexture.NO_OVERLAY, 0);

        for (SubmitNodeCollection coll : storage.getSubmitsPerOrder().values()) {
            readItemSubmits(coll, sink);
            readModelSubmits(coll, sink);
            readModelPartSubmits(coll, sink);
            readCustomGeometrySubmits(coll, sink);
        }

        pose.popPose();
    }

    private PackedQuad normalizeSpriteForCaching(PackedQuad quad) {
        TextureAtlasSprite src = quad.sprite();
        if (!sprites.isNotBlockAtlas(src)) {
            return quad;
        }
        TextureAtlasSprite dst = sprites.tryGetBlockItemSprite(src);
        if (dst == null) {
            dst = sprites.missingNoOrNull();
        }
        return remapPackedQuadToSprite(quad, src, dst);
    }

    private void readItemSubmits(SubmitNodeCollection coll, Sink sink) {
        for (SubmitNodeStorage.ItemSubmit submit : coll.getItemSubmits()) {
            Matrix4f mat = submit.pose().pose();
            int[] tints = submit.tintLayers();
            RenderType rtObj = submit.renderType();

            for (BakedQuad quad : submit.quads()) {
                sink.accept(transformQuadToPacked(quad, mat), rtObj, tints);
            }
        }
    }

    private void readModelSubmits(SubmitNodeCollection coll, Sink sink) {
        ModelFeatureRenderer.Storage storage = coll.getModelSubmits();
        var acc = (ModelFeatureRendererStorageAccessor) storage;

        for (var e : acc.getOpaqueModelSubmits().entrySet()) {
            RenderType rtObj = e.getKey();
            for (SubmitNodeStorage.ModelSubmit<?> sub : e.getValue()) {
                tessellateModelSubmit(rtObj, sub, sink);
            }
        }

        for (SubmitNodeStorage.TranslucentModelSubmit<?> t : acc.getTranslucentModelSubmits()) {
            tessellateModelSubmit(t.renderType(), t.modelSubmit(), sink);
        }
    }

    private <S> void tessellateModelSubmit(RenderType rtObj, SubmitNodeStorage.ModelSubmit<S> sub, Sink sink) {
        RecordingVertexConsumer rec = tlRec.get();
        rec.clear();

        PoseStack ps = tlTessPose.get();
        resetPoseToIdentity(ps);
        ps.last().set(sub.pose());

        TextureAtlasSprite sprite = sub.sprite();

        TextureAtlasSprite inferred = null;
        if (sprite == null) {
            var tex = rt.tryExtractTextureId(rtObj);
            inferred = (tex != null) ? sprites.tryResolveEntitySprite(tex) : null;
            rec.setActiveSprite(inferred);
        } else {
            rec.setActiveSprite(null);
        }

        VertexConsumer out = (sprite != null) ? sprite.wrap(rec) : rec;

        Model<? super S> model = sub.model();
        model.setupAnim(sub.state());
        model.renderToBuffer(ps, out, sub.lightCoords(), sub.overlayCoords(), sub.tintedColor());

        rec.flush();
        if (inferred != null) {
            rec.remapUvsToActiveSpriteIfPresent();
        }

        TextureAtlasSprite finalSprite = (sprite != null)
                ? sprite
                : (inferred != null ? inferred : sprites.missingNoOrNull());
        emitRecordedAsQuads(rtObj, rec.vertices(), finalSprite, sub.tintedColor(), sink);
    }

    private void readModelPartSubmits(SubmitNodeCollection coll, Sink sink) {
        ModelPartFeatureRenderer.Storage storage = coll.getModelPartSubmits();
        var acc = (ModelPartFeatureRendererStorageAccessor) storage;

        for (var e : acc.getModelPartSubmits().entrySet()) {
            RenderType rtObj = e.getKey();
            for (SubmitNodeStorage.ModelPartSubmit sub : e.getValue()) {
                tessellateModelPartSubmit(rtObj, sub, sink);
            }
        }
    }

    private void tessellateModelPartSubmit(RenderType rtObj, SubmitNodeStorage.ModelPartSubmit sub, Sink sink) {
        RecordingVertexConsumer rec = tlRec.get();
        rec.clear();

        PoseStack ps = tlTessPose.get();
        resetPoseToIdentity(ps);
        ps.last().set(sub.pose());

        TextureAtlasSprite sprite = sub.sprite();
        VertexConsumer out = (sprite != null) ? sprite.wrap(rec) : rec;

        ModelPart part = sub.modelPart();
        part.render(ps, out, sub.lightCoords(), sub.overlayCoords(), sub.tintedColor());

        rec.flush();

        TextureAtlasSprite finalSprite = (sprite != null) ? sprite : sprites.missingNoOrNull();

        emitRecordedAsQuads(rtObj, rec.vertices(), finalSprite, sub.tintedColor(), sink);
    }

    private void readCustomGeometrySubmits(SubmitNodeCollection coll, Sink sink) {
        CustomFeatureRenderer.Storage storage = coll.getCustomGeometrySubmits();
        var acc = (CustomFeatureRendererStorageAccessor) storage;

        for (var e : acc.getCustomGeometrySubmits().entrySet()) {
            RenderType rtObj = e.getKey();
            for (SubmitNodeStorage.CustomGeometrySubmit sub : e.getValue()) {
                tessellateCustomSubmit(rtObj, sub, sink);
            }
        }
    }

    private void tessellateCustomSubmit(RenderType rtObj, SubmitNodeStorage.CustomGeometrySubmit sub, Sink sink) {
        RecordingVertexConsumer rec = tlRec.get();
        rec.clear();

        sub.customGeometryRenderer().render(sub.pose(), rec);

        rec.flush();
        emitRecordedAsTris(rtObj, rec.vertices(), sink);
    }

    /* ---------- recorded emission ---------- */

    private void emitRecordedAsQuads(RenderType rtObj, List<RecordingVertexConsumer.Vtx> verts, TextureAtlasSprite sprite, int tintedColor, Sink sink) {
        if (verts.isEmpty()) return;

        int[] tintLayers = tlOneTint.get();
        tintLayers[0] = tintedColor;

        int n = verts.size();
        if ((n & 3) != 0) {
            emitRecordedAsTris(rtObj, verts, sink);
            return;
        }

        for (int i = 0; i + 3 < n; i += 4) {
            var a = verts.get(i);
            var b = verts.get(i + 1);
            var c = verts.get(i + 2);
            var d = verts.get(i + 3);

            Direction dir = guessDirection(a, b, c);
            PackedQuad q = makePackedQuad(a, b, c, d, dir, sprite, 0);
            sink.accept(q, rtObj, tintLayers);
        }
    }

    private void emitRecordedAsTris(RenderType rtObj, List<RecordingVertexConsumer.Vtx> verts, Sink sink) {
        if (verts.isEmpty()) return;

        TextureAtlasSprite sprite = sprites.missingNoOrNull();

        int n = verts.size();
        for (int i = 0; i + 2 < n; i += 3) {
            var a = verts.get(i);
            var b = verts.get(i + 1);
            var c = verts.get(i + 2);

            Direction dir = guessDirection(a, b, c);
            PackedQuad q = makePackedQuad(a, b, c, c, dir, sprite, -1);
            sink.accept(q, rtObj, null);
        }
    }

    private static PackedQuad makePackedQuad(
            RecordingVertexConsumer.Vtx a,
            RecordingVertexConsumer.Vtx b,
            RecordingVertexConsumer.Vtx c,
            RecordingVertexConsumer.Vtx d,
            Direction dir,
            TextureAtlasSprite sprite,
            int tintIndex
    ) {
        long uv0 = UVPair.pack(a.u, a.v);
        long uv1 = UVPair.pack(b.u, b.v);
        long uv2 = UVPair.pack(c.u, c.v);
        long uv3 = UVPair.pack(d.u, d.v);

        return new PackedQuad(
                a.x, a.y, a.z,
                b.x, b.y, b.z,
                c.x, c.y, c.z,
                d.x, d.y, d.z,

                uv0, uv1, uv2, uv3,
                dir,
                true,
                0,
                tintIndex,
                sprite,
                null
        );
    }

    private static Direction guessDirection(RecordingVertexConsumer.Vtx a, RecordingVertexConsumer.Vtx b, RecordingVertexConsumer.Vtx c) {
        float ax = b.x - a.x, ay = b.y - a.y, az = b.z - a.z;
        float bx = c.x - a.x, by = c.y - a.y, bz = c.z - a.z;

        float nx = ay * bz - az * by;
        float ny = az * bx - ax * bz;
        float nz = ax * by - ay * bx;

        float anx = Math.abs(nx), any = Math.abs(ny), anz = Math.abs(nz);
        if (anx >= any && anx >= anz) return (nx >= 0) ? Direction.EAST : Direction.WEST;
        if (any >= anx && any >= anz) return (ny >= 0) ? Direction.UP : Direction.DOWN;
        return (nz >= 0) ? Direction.SOUTH : Direction.NORTH;
    }

    private static void resetPoseToIdentity(PoseStack ps) {
        ps.last().pose().identity();
        ps.last().normal().identity();
    }

    private static PackedQuad transformQuadToPacked(BakedQuad q, Matrix4f m) {
        V4 v = TL_V4.get();

        Vector3f p0 = v.a.set(q.position0().x(), q.position0().y(), q.position0().z());
        Vector3f p1 = v.b.set(q.position1().x(), q.position1().y(), q.position1().z());
        Vector3f p2 = v.c.set(q.position2().x(), q.position2().y(), q.position2().z());
        Vector3f p3 = v.d.set(q.position3().x(), q.position3().y(), q.position3().z());

        m.transformPosition(p0);
        m.transformPosition(p1);
        m.transformPosition(p2);
        m.transformPosition(p3);

        return new PackedQuad(
                p0.x, p0.y, p0.z,
                p1.x, p1.y, p1.z,
                p2.x, p2.y, p2.z,
                p3.x, p3.y, p3.z,

                q.packedUV0(),
                q.packedUV1(),
                q.packedUV2(),
                q.packedUV3(),

                q.direction(),
                q.shade(),
                q.lightEmission(),
                q.tintIndex(),
                q.sprite(),
                null
        );
    }

    private static PackedQuad remapPackedQuadToSprite(PackedQuad q, TextureAtlasSprite src, TextureAtlasSprite dst) {
        long uv0 = remapPackedUv(q.uv0(), src, dst);
        long uv1 = remapPackedUv(q.uv1(), src, dst);
        long uv2 = remapPackedUv(q.uv2(), src, dst);
        long uv3 = remapPackedUv(q.uv3(), src, dst);

        return new PackedQuad(
                q.x0(), q.y0(), q.z0(),
                q.x1(), q.y1(), q.z1(),
                q.x2(), q.y2(), q.z2(),
                q.x3(), q.y3(), q.z3(),
                uv0, uv1, uv2, uv3,
                q.dir(),
                q.shade(),
                q.lightEmission(),
                q.tintIndex(),
                dst,
                dst
        );
    }

    private static long remapPackedUv(long packed, TextureAtlasSprite src, TextureAtlasSprite dst) {
        float u = UVPair.unpackU(packed);
        float v = UVPair.unpackV(packed);

        float su0 = src.getU0(), su1 = src.getU1();
        float sv0 = src.getV0(), sv1 = src.getV1();

        float du0 = dst.getU0(), du1 = dst.getU1();
        float dv0 = dst.getV0(), dv1 = dst.getV1();

        float ru = (su1 != su0) ? (u - su0) / (su1 - su0) : 0.0f;
        float rv = (sv1 != sv0) ? (v - sv0) / (sv1 - sv0) : 0.0f;

        float nu = du0 + ru * (du1 - du0);
        float nv = dv0 + rv * (dv1 - dv0);

        return UVPair.pack(nu, nv);
    }
}
