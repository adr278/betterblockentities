package betterblockentities.client.chunk.pipeline;

/* local */
import betterblockentities.client.chunk.pipeline.record.RecordingVertexConsumer;
import betterblockentities.client.chunk.util.QuadTransform;

/* minecraft */
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.renderer.LevelRenderer;
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
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ShelfBlock;
import net.minecraft.world.level.block.entity.ShelfBlockEntity;
import net.minecraft.world.phys.AABB;

/* sodium */
import net.caffeinemc.mods.sodium.client.render.model.MutableQuadViewImpl;
import net.caffeinemc.mods.sodium.client.render.model.SodiumQuadAtlas;

/* java/misc */
import org.joml.Matrix4f;
import org.joml.Vector3f;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class ShelfItemEmitter {
    private ShelfItemEmitter() {}

    private static final ThreadLocal<ItemStackRenderState> TL_STATE =
            ThreadLocal.withInitial(ItemStackRenderState::new);

    private static final ThreadLocal<SubmitNodeStorage> TL_STORAGE =
            ThreadLocal.withInitial(SubmitNodeStorage::new);

    private static final ThreadLocal<RecordingVertexConsumer> TL_REC =
            ThreadLocal.withInitial(RecordingVertexConsumer::new);

    // Reflection caches for package-private storage fields
    private static volatile Field F_MODEL_OPAQUE_MAP;
    private static volatile Field F_MODEL_TRANSLUCENT_LIST;
    private static volatile Field F_MODELPART_MAP;
    private static volatile Field F_CUSTOM_MAP;

    public static void emit(MutableQuadViewImpl emitter, BlockPos pos, Level level, ShelfBlockEntity shelf) {
        final int light = LevelRenderer.getLightColor(level, pos);

        for (int slot = 0; slot < 3; slot++) {
            ItemStack stack = shelf.getItems().get(slot);
            if (stack.isEmpty()) continue;

            captureShelfItemGeometry(level, shelf, pos, slot, stack, light,
                    (quad, renderTypeObj, tintLayers) -> {
                        emitter.fromBakedQuad(quad);
                        emitter.setCullFace(null);
                        emitter.setRenderType(mapLayer(renderTypeObj));

                        // If this quad uses an item sprite, swap to stitched sprite and remap UVs
                        TextureAtlasSprite srcSprite = quad.sprite();
                        if (!AtlasIds.BLOCKS.equals(srcSprite.atlasLocation())) {
                            TextureAtlasSprite stitched = tryGetStitchedBlockItemSprite(srcSprite);
                            if (stitched != null) {
                                QuadTransform.swapSprite(stitched, emitter);
                                emitter.cachedSprite(stitched);
                            }
                        }

                        emitter.setQuadAtlas(SodiumQuadAtlas.BLOCK);

                        int ti = quad.tintIndex();
                        if (ti >= 0 && tintLayers != null && ti < tintLayers.length) {
                            int tint = tintLayers[ti];
                            for (int v = 0; v < 4; v++) emitter.setColor(v, tint);
                        }

                        emitter.emitDirectly();
                        emitter.clear();
                    });
        }
    }

    @FunctionalInterface private interface Sink {
        void accept(BakedQuad quad, Object renderTypeObj, int[] tintLayers);
    }

    private static void captureShelfItemGeometry(
            Level level,
            ShelfBlockEntity shelf,
            BlockPos pos,
            int slot,
            ItemStack stack,
            int light,
            Sink sink
    ) {
        Minecraft mc = Minecraft.getInstance();
        ItemModelResolver resolver = mc.getItemModelResolver();

        ItemStackRenderState state = TL_STATE.get();
        state.clear();

        int seedBase = (int) (pos.asLong() ^ 0x9E3779B9L);
        resolver.updateForTopItem(
                state,
                stack,
                ItemDisplayContext.ON_SHELF,
                level,
                shelf,
                seedBase + slot
        );

        if (state.isEmpty()) return;

        PoseStack pose = new PoseStack();

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

        SubmitNodeStorage storage = TL_STORAGE.get();
        storage.clear();

        // Populate ALL submit types (item/model/modelpart/custom)
        state.submit(pose, storage, light, OverlayTexture.NO_OVERLAY, 0);

        // Read back submits
        for (SubmitNodeCollection coll : storage.getSubmitsPerOrder().values()) {
            readItemSubmits(coll, sink);
            readModelSubmits(coll, sink);
            readModelPartSubmits(coll, sink);
            readCustomGeometrySubmits(coll, sink);
        }
        pose.popPose();
    }

    private static void readItemSubmits(SubmitNodeCollection coll, Sink sink) {
        for (SubmitNodeStorage.ItemSubmit submit : coll.getItemSubmits()) {
            Matrix4f mat = submit.pose().pose();
            int[] tints = submit.tintLayers();
            RenderType rt = submit.renderType();

            for (BakedQuad quad : submit.quads()) {
                sink.accept(transformQuadPositions(quad, mat), rt, tints);
            }
        }
    }

    @SuppressWarnings("unchecked") private static void readModelSubmits(SubmitNodeCollection coll, Sink sink) {
        ModelFeatureRenderer.Storage storage = coll.getModelSubmits();

        try {
            Map<RenderType, List<SubmitNodeStorage.ModelSubmit<?>>> opaque =
                    (Map<RenderType, List<SubmitNodeStorage.ModelSubmit<?>>>) modelOpaqueField().get(storage);

            List<SubmitNodeStorage.TranslucentModelSubmit<?>> translucent =
                    (List<SubmitNodeStorage.TranslucentModelSubmit<?>>) modelTranslucentField().get(storage);

            // Opaque buckets
            for (var e : opaque.entrySet()) {
                RenderType rt = e.getKey();
                for (SubmitNodeStorage.ModelSubmit<?> sub : e.getValue()) {
                    tessellateModelSubmit(rt, sub, sink);
                }
            }

            // Translucent list includes render type per entry
            for (SubmitNodeStorage.TranslucentModelSubmit<?> t : translucent) {
                tessellateModelSubmit(t.renderType(), t.modelSubmit(), sink);
            }

        } catch (Throwable ignored) {}
    }

    private static <S> void tessellateModelSubmit(RenderType rt, SubmitNodeStorage.ModelSubmit<S> sub, Sink sink) {
        RecordingVertexConsumer rec = TL_REC.get();
        rec.clear();

        PoseStack ps = new PoseStack();
        ps.last().set(sub.pose());

        TextureAtlasSprite sprite = sub.sprite();

        // If vanilla didn't provide a sprite (common for entity render types),
        // try to recover the texture id from the RenderType and map it to a stitched BLOCK sprite.
        if (sprite == null) {
            Identifier tex = tryExtractTextureId(rt); // minecraft:textures/entity/
            TextureAtlasSprite stitched = (tex != null) ? tryGetStitchedEntitySprite(tex) : null; // minecraft:entity/
            rec.setActiveSprite(stitched);
        } else {
            rec.setActiveSprite(null); // not needed, sprite.wrap handles it
        }

        VertexConsumer out = wrapSprite(sprite, rec);

        Model<? super S> model = sub.model();
        model.setupAnim(sub.state());
        model.renderToBuffer(ps, out, sub.lightCoords(), sub.overlayCoords(), sub.tintedColor());

        rec.flush();

        // If we had to stitch an entity texture into the block atlas, expand UVs now.
        rec.remapUvsToActiveSpriteIfPresent();

        TextureAtlasSprite finalSprite = (sprite != null) ? sprite : rec.getActiveSprite();
        emitRecordedAsQuads(rt, rec.vertices(), finalSprite, sub.tintedColor(), sink);
    }

    @SuppressWarnings("unchecked") private static void readModelPartSubmits(SubmitNodeCollection coll, Sink sink) {
        ModelPartFeatureRenderer.Storage storage = coll.getModelPartSubmits();

        try {
            Map<RenderType, List<SubmitNodeStorage.ModelPartSubmit>> map =
                    (Map<RenderType, List<SubmitNodeStorage.ModelPartSubmit>>) modelPartMapField().get(storage);

            for (var e : map.entrySet()) {
                RenderType rt = e.getKey();
                for (SubmitNodeStorage.ModelPartSubmit sub : e.getValue()) {
                    tessellateModelPartSubmit(rt, sub, sink);
                }
            }

        } catch (Throwable ignored) {}
    }

    private static void tessellateModelPartSubmit(RenderType rt, SubmitNodeStorage.ModelPartSubmit sub, Sink sink) {
        RecordingVertexConsumer rec = TL_REC.get();
        rec.clear();

        PoseStack ps = new PoseStack();
        ps.last().set(sub.pose());

        VertexConsumer out = wrapSprite(sub.sprite(), rec);

        ModelPart part = sub.modelPart();
        part.render(ps, out, sub.lightCoords(), sub.overlayCoords(), sub.tintedColor());

        rec.flush();

        emitRecordedAsQuads(rt, rec.vertices(), sub.sprite(), sub.tintedColor(), sink);
    }

    @SuppressWarnings("unchecked")
    private static void readCustomGeometrySubmits(SubmitNodeCollection coll, Sink sink) {
        CustomFeatureRenderer.Storage storage = coll.getCustomGeometrySubmits();

        try {
            Map<RenderType, List<SubmitNodeStorage.CustomGeometrySubmit>> map =
                    (Map<RenderType, List<SubmitNodeStorage.CustomGeometrySubmit>>) customMapField().get(storage);

            for (var e : map.entrySet()) {
                RenderType rt = e.getKey();
                for (SubmitNodeStorage.CustomGeometrySubmit sub : e.getValue()) {
                    tessellateCustomSubmit(rt, sub, sink);
                }
            }
        } catch (Throwable ignored) {}
    }

    private static void tessellateCustomSubmit(RenderType rt, SubmitNodeStorage.CustomGeometrySubmit sub, Sink sink) {
        RecordingVertexConsumer rec = TL_REC.get();
        rec.clear();

        sub.customGeometryRenderer().render(sub.pose(), rec);

        rec.flush();

        emitRecordedAsTris(rt, rec.vertices(), sink);
    }

    private static void emitRecordedAsQuads(
            RenderType rt,
            List<RecordingVertexConsumer.Vtx> verts,
            TextureAtlasSprite spriteOrNull,
            int tintedColor,
            Sink sink
    ) {
        if (verts.isEmpty()) return;

        int[] tintLayers = new int[]{ tintedColor };
        int tintIndex = 0;

        TextureAtlasSprite sprite = (spriteOrNull != null) ? spriteOrNull : missingNo();

        int n = verts.size();
        if ((n & 3) != 0) {
            emitRecordedAsTris(rt, verts, sink);
            return;
        }

        for (int i = 0; i + 3 < n; i += 4) {
            var a = verts.get(i);
            var b = verts.get(i + 1);
            var c = verts.get(i + 2);
            var d = verts.get(i + 3);

            Direction dir = guessDirection(a, b, c);
            BakedQuad q = makeBakedQuad(a, b, c, d, tintIndex, dir, sprite);
            sink.accept(q, rt, tintLayers);
        }
    }

    private static void emitRecordedAsTris(RenderType rt, List<RecordingVertexConsumer.Vtx> verts, Sink sink) {
        if (verts.isEmpty()) return;

        TextureAtlasSprite sprite = missingNo();

        int n = verts.size();
        for (int i = 0; i + 2 < n; i += 3) {
            var a = verts.get(i);
            var b = verts.get(i + 1);
            var c = verts.get(i + 2);

            Direction dir = guessDirection(a, b, c);
            BakedQuad q = makeBakedQuad(a, b, c, c, -1, dir, sprite);
            sink.accept(q, rt, null);
        }
    }

    private static BakedQuad makeBakedQuad(
            RecordingVertexConsumer.Vtx a,
            RecordingVertexConsumer.Vtx b,
            RecordingVertexConsumer.Vtx c,
            RecordingVertexConsumer.Vtx d,
            int tintIndex,
            Direction dir,
            TextureAtlasSprite sprite
    ) {
        Vector3f p0 = new Vector3f(a.x, a.y, a.z);
        Vector3f p1 = new Vector3f(b.x, b.y, b.z);
        Vector3f p2 = new Vector3f(c.x, c.y, c.z);
        Vector3f p3 = new Vector3f(d.x, d.y, d.z);

        long uv0 = UVPair.pack(a.u, a.v);
        long uv1 = UVPair.pack(b.u, b.v);
        long uv2 = UVPair.pack(c.u, c.v);
        long uv3 = UVPair.pack(d.u, d.v);

        return new BakedQuad(
                p0, p1, p2, p3,
                uv0, uv1, uv2, uv3,
                tintIndex,
                dir,
                sprite,
                true,
                0
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

    private static TextureAtlasSprite missingNo() {
        return Minecraft.getInstance()
                .getAtlasManager()
                .getAtlasOrThrow(AtlasIds.BLOCKS)
                .getSprite(Identifier.withDefaultNamespace("missingno"));
    }

    private static VertexConsumer wrapSprite(TextureAtlasSprite sprite, VertexConsumer base) {
        if (sprite == null) return base;
        return sprite.wrap(base);
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

    private static ChunkSectionLayer mapLayer(Object renderTypeObj) {
        if (renderTypeObj == null) return ChunkSectionLayer.CUTOUT;
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

    private static TextureAtlasSprite tryGetStitchedBlockItemSprite(TextureAtlasSprite srcSprite) {
        // Sprite id as used in its own atlas (often "minecraft:item/...")
        Identifier srcId = srcSprite.contents().name();

        String p = srcId.getPath();
        if (!(p.startsWith("item/") || p.startsWith("minecraft:item/") || p.contains("item/"))) {
        }

        var blockAtlas = Minecraft.getInstance()
                .getAtlasManager()
                .getAtlasOrThrow(AtlasIds.BLOCKS);

        TextureAtlasSprite missing = blockAtlas.getSprite(Identifier.withDefaultNamespace("missingno"));

        for (Identifier cand : blockAtlasItemCandidates(srcId)) {
            TextureAtlasSprite s = blockAtlas.getSprite(cand);
            if (s != missing && s.contents() != missing.contents()) {
                return s;
            }
        }

        return null;
    }

    private static List<Identifier> blockAtlasItemCandidates(Identifier srcId) {
        String ns = srcId.getNamespace();
        String path = srcId.getPath();

        java.util.ArrayList<Identifier> out = new java.util.ArrayList<>(6);

        // important: same id in the BLOCK atlas
        out.add(srcId);

        // Normalize common forms
        // If the path already contains "item/...", extract the logical name part.
        String name;
        if (path.startsWith("item/")) {
            name = path.substring("item/".length());
        } else if (path.contains("item/")) {
            name = path.substring(path.indexOf("item/") + "item/".length());
        } else {
            name = path;
        }

        addIfParse(out, ns, "item/" + name);
        addIfParse(out, "minecraft", "item/" + ns + "/" + name);

        return out;
    }

    private static void addIfParse(List<Identifier> out, String ns, String path) {
        Identifier id = Identifier.tryParse(ns + ":" + path);
        if (id != null) out.add(id);
    }

    private static Identifier stitchedId(Identifier originalItemId) {
        String ns = originalItemId.getNamespace();
        String path = originalItemId.getPath();
        String name = path.substring("item/".length());

        Identifier id = Identifier.tryParse("minecraft:item/" + name + ":" + ns + "/" + name);
        return (id != null) ? id : originalItemId;
    }

    private static Field modelOpaqueField() {
        Field f = F_MODEL_OPAQUE_MAP;
        if (f != null) return f;
        try {
            f = ModelFeatureRenderer.Storage.class.getDeclaredField("opaqueModelSubmits");
            f.setAccessible(true);
            F_MODEL_OPAQUE_MAP = f;
            return f;
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    private static Field modelTranslucentField() {
        Field f = F_MODEL_TRANSLUCENT_LIST;
        if (f != null) return f;
        try {
            f = ModelFeatureRenderer.Storage.class.getDeclaredField("translucentModelSubmits");
            f.setAccessible(true);
            F_MODEL_TRANSLUCENT_LIST = f;
            return f;
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    private static Field modelPartMapField() {
        Field f = F_MODELPART_MAP;
        if (f != null) return f;
        try {
            f = ModelPartFeatureRenderer.Storage.class.getDeclaredField("modelPartSubmits");
            f.setAccessible(true);
            F_MODELPART_MAP = f;
            return f;
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    private static Field customMapField() {
        Field f = F_CUSTOM_MAP;
        if (f != null) return f;
        try {
            f = CustomFeatureRenderer.Storage.class.getDeclaredField("customGeometrySubmits");
            f.setAccessible(true);
            F_CUSTOM_MAP = f;
            return f;
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    private static volatile java.lang.reflect.Field F_RT_STATE;        // RenderType#state
    private static volatile java.lang.reflect.Field F_RS_TEXTURES;     // RenderSetup#textures
    private static volatile java.lang.reflect.Field F_TB_LOCATION;     // TextureBinding#location

    private static Identifier tryExtractTextureId(RenderType rt) {
        try {
            java.lang.reflect.Field fState = F_RT_STATE;
            if (fState == null) {
                fState = RenderType.class.getDeclaredField("state");
                fState.setAccessible(true);
                F_RT_STATE = fState;
            }

            Object renderSetup = fState.get(rt); // net.minecraft.client.renderer.rendertype.RenderSetup
            if (renderSetup == null) return null;

            java.lang.reflect.Field fTextures = F_RS_TEXTURES;
            if (fTextures == null) {
                fTextures = renderSetup.getClass().getDeclaredField("textures");
                fTextures.setAccessible(true);
                F_RS_TEXTURES = fTextures;
            }

            @SuppressWarnings("unchecked")
            Map<String, ?> textures = (Map<String, ?>) fTextures.get(renderSetup);
            if (textures == null || textures.isEmpty()) return null;

            // Most entity types bind a single main texture (often Sampler0)
            Object binding = textures.values().iterator().next();
            if (binding == null) return null;

            java.lang.reflect.Field fLoc = F_TB_LOCATION;
            if (fLoc == null) {
                fLoc = binding.getClass().getDeclaredField("location");
                fLoc.setAccessible(true);
                F_TB_LOCATION = fLoc;
            }

            return (Identifier) fLoc.get(binding);
        } catch (Throwable t) {
            return null;
        }
    }

    private static TextureAtlasSprite tryGetStitchedEntitySprite(Identifier textureId) {
        // Expect: namespace:textures/entity/....png
        String ns = textureId.getNamespace();
        String path = textureId.getPath();

        if (!path.startsWith("textures/") || !path.endsWith(".png")) return null;

        // Strip "textures/" and ".png"
        String core = path.substring("textures/".length(), path.length() - ".png".length());
        Identifier spriteId = Identifier.tryParse(ns + ":" + core);
        if (spriteId == null) return null;

        var blockAtlas = Minecraft.getInstance()
                .getAtlasManager()
                .getAtlasOrThrow(AtlasIds.BLOCKS);

        TextureAtlasSprite missing = blockAtlas.getSprite(Identifier.withDefaultNamespace("missingno"));
        TextureAtlasSprite s = blockAtlas.getSprite(spriteId);

        if (s == missing || s.contents() == missing.contents()) return null;
        return s;
    }
}
