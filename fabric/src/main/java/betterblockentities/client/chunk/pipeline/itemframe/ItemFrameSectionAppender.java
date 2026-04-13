package betterblockentities.client.chunk.pipeline.itemframe;

/* local */
import betterblockentities.client.chunk.pipeline.shelf.CacheKeys;
import betterblockentities.client.render.immediate.entity.ItemFrameRuntimeHelper;

/* mojang */
import com.mojang.blaze3d.vertex.PoseStack;

/* minecraft */
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.entity.ItemFrameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/* sodium */
import net.caffeinemc.mods.sodium.client.render.model.MutableQuadViewImpl;

/* java */
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/* misc */
import org.jspecify.annotations.Nullable;

public record ItemFrameSectionAppender(
        BlockPos sectionOrigin,
        List<Entry> entries,
        Map<BlockPos, List<Entry>> entriesBySupportPos
) {
    public static final int NO_TINT = CacheKeys.NO_TINT;

    public record Entry(
            int entityId,
            long renderSignature,
            BlockPos supportPos,
            Vec3 entityPos,
            Direction facing,
            int rotation,
            boolean glowFrame,
            boolean invisible,
            int frameLight,
            int itemLight,
            int mapLight,
            ItemFrameContentRenderMode contentRenderMode,
            ItemFrameItemModelBuilder.CapturedMesh frameMesh,
            ItemFrameItemModelBuilder.CapturedMesh itemMesh,
            @Nullable MapId mapId
    ) {}

    public static @Nullable ItemFrameSectionAppender capture(Level level, BlockPos sectionOrigin) {
        if (!ItemFrameEligibility.optimizationEnabled()) return null;

        SectionPos sectionPos = SectionPos.of(sectionOrigin);
        AABB bounds = new AABB(
                sectionOrigin.getX() - 1.0D,
                sectionOrigin.getY() - 1.0D,
                sectionOrigin.getZ() - 1.0D,
                sectionOrigin.getX() + 17.0D,
                sectionOrigin.getY() + 17.0D,
                sectionOrigin.getZ() + 17.0D
        );

        LinkedHashMap<Integer, ItemFrame> framesById = new LinkedHashMap<>();

        collectIndexedFrames(level, sectionPos, framesById);

        // Only use the local entity scan during bootstrap/recovery.
        // After that, the section registry and global recovery seed should own discovery.
        if (ItemFrameRuntimeHelper.shouldCollectLiveFramesDuringSectionCapture()) {
            collectLiveFrames(level, sectionOrigin, bounds, framesById);
        }

        if (framesById.isEmpty()) return null;

        List<ItemFrame> frames = new ArrayList<>(framesById.values());
        frames.sort(Comparator.comparingInt(ItemFrame::getId));

        ArrayList<Entry> entries = new ArrayList<>(frames.size());
        HashMap<BlockPos, List<Entry>> entriesBySupportPos = new HashMap<>(frames.size());

        for (ItemFrame frame : frames) {
            Entry entry = capture(frame);
            entries.add(entry);
            entriesBySupportPos
                    .computeIfAbsent(entry.supportPos(), ignored -> new ArrayList<>(1))
                    .add(entry);
        }

        HashMap<BlockPos, List<Entry>> immutableEntriesBySupportPos =
                new HashMap<>(entriesBySupportPos.size());

        entriesBySupportPos.forEach((supportPos, supportEntries) ->
                immutableEntriesBySupportPos.put(
                        supportPos.immutable(),
                        List.copyOf(supportEntries)
                )
        );

        return new ItemFrameSectionAppender(
                sectionOrigin.immutable(),
                List.copyOf(entries),
                Map.copyOf(immutableEntriesBySupportPos)
        );
    }

    private static void collectIndexedFrames(
            Level level,
            SectionPos sectionPos,
            LinkedHashMap<Integer, ItemFrame> framesById
    ) {
        for (ItemFrame frame : ItemFrameSectionRegistry.framesForSection(sectionPos)) {
            if (isStaleIndexedFrame(level, frame)) {
                ItemFrameMapIndex.remove(frame.getId());
                ItemFrameSectionRegistry.remove(frame.getId());

                // Don't call ItemFrameRemovalTracker.markRemoved() here.
                // A stale registry reference is not proof that the entity was
                // actually removed. Flashback/render-only item frames can be
                // missing from ClientLevel.getEntity() while still being valid
                // in entitiesForRendering().
                continue;
            }

            if (SectionPos.of(ItemFrameRuntimeHelper.supportPos(frame)).asLong() != sectionPos.asLong()) {
                continue;
            }

            framesById.put(frame.getId(), frame);
        }
    }

    private static boolean isStaleIndexedFrame(Level level, ItemFrame frame) {
        int entityId = frame.getId();

        if (frame.isRemoved()) {
            ItemFrameRemovalTracker.markRemoved(entityId);
            return true;
        }

        if (!(level instanceof ClientLevel clientLevel)) return ItemFrameRemovalTracker.isRemoved(entityId);

        Entity entity = clientLevel.getEntity(entityId);
        if (entity == frame && !entity.isRemoved()) {
            ItemFrameRemovalTracker.clearIfRemoved(entityId);
            return false;
        }

        // Flashback/render-only path.
        for (Entity renderEntity : clientLevel.entitiesForRendering()) {
            if (renderEntity == frame && !renderEntity.isRemoved()) {
                ItemFrameRemovalTracker.clearIfRemoved(entityId);
                return false;
            }
        }

        return ItemFrameRemovalTracker.isRemoved(entityId) || entity != frame;
    }

    private static void collectLiveFrames(
            Level level,
            BlockPos sectionOrigin,
            AABB bounds,
            LinkedHashMap<Integer, ItemFrame> framesById
    ) {
        /*
         * Local entity lookup only.
         *
         * Render-only/replay entities are recovered globally by
         * MapPageCache.seedMapIndexFromLiveFrames(), not by scanning the full
         * render entity list once per section capture.
         */
        for (ItemFrame frame : level.getEntitiesOfClass(
                ItemFrame.class,
                bounds,
                candidate -> ItemFrameRuntimeHelper.isSectionSupportPos(
                        sectionOrigin,
                        ItemFrameRuntimeHelper.supportPos(candidate)
                )
        )) {
            if (frame.isRemoved()) continue;
            acceptLiveFrame(frame, framesById);
        }
    }

    private static Entry capture(ItemFrame frame) {
        boolean glowFrame = frame.is(EntityType.GLOW_ITEM_FRAME);
        boolean invisible = frame.isInvisible();

        ItemFrameRemovalTracker.markAdded(frame.getId());

        ItemFrameEligibility.Evaluation evaluation =
                ItemFrameEligibility.evaluateForSectionCapture(frame);

        MapId mapId = evaluation.mapId();
        if (mapId != null && ItemFrameEligibility.isFrameMeshSupported(frame, mapId)) {
            ItemFrameMapIndex.upsert(frame, mapId, true);
            ItemFrameSectionRegistry.upsert(frame);
        }

        ItemFrameItemModelBuilder.CapturedMesh frameMesh = null;
        if (!invisible) frameMesh = ItemFrameModelCapture.getFrameMesh(glowFrame, mapId != null);

        BlockPos lightPos = BlockPos.containing(frame.getLightProbePosition(1.0F));
        int blockLight = frame.level().getBrightness(LightLayer.BLOCK, lightPos);
        if (glowFrame) blockLight = Math.max(ItemFrameRenderer.GLOW_FRAME_BRIGHTNESS, blockLight);

        int skyLight = frame.level().getBrightness(LightLayer.SKY, lightPos);
        int frameLight = LightCoordsUtil.pack(blockLight, skyLight);
        int itemLight = ItemFrameRenderHelper.getItemLight(glowFrame, frameLight);
        int mapLight = ItemFrameRenderHelper.getMapLight(glowFrame, frameLight);

        return new Entry(
                frame.getId(),
                ItemFrameEligibility.computeRenderSignature(frame, evaluation),
                ItemFrameRuntimeHelper.supportPos(frame),
                frame.position(),
                frame.getDirection(),
                frame.getRotation(),
                glowFrame,
                invisible,
                frameLight,
                itemLight,
                mapLight,
                evaluation.contentRenderMode(),
                frameMesh,
                evaluation.itemCapture().mesh(),
                mapId
        );
    }

    public void emitForSupportPos(BlockPos supportPos, MutableQuadViewImpl emitter) {
        List<Entry> supportEntries = this.entriesBySupportPos.get(supportPos);
        if (supportEntries == null || supportEntries.isEmpty()) return;

        PoseStack pose = new PoseStack();

        for (Entry entry : supportEntries) {
            if (ItemFrameRemovalTracker.isRemoved(entry.entityId())) continue;

            pose.pushPose();
            pose.translate(
                    entry.entityPos().x - supportPos.getX(),
                    entry.entityPos().y - supportPos.getY(),
                    entry.entityPos().z - supportPos.getZ()
            );
            ItemFrameRenderHelper.applyFacingPose(pose, entry.facing());

            if (!entry.invisible && entry.frameMesh() != null) emitCapturedMesh(entry.frameMesh(), pose, emitter, entry.frameLight());

            pose.pushPose();
            ItemFrameRenderHelper.translateToContentPlane(pose, entry.invisible());

            if (entry.contentRenderMode() == ItemFrameContentRenderMode.NONE
                    && entry.itemMesh() != null
                    && !entry.itemMesh().isEmpty()) {
                ItemFrameRenderHelper.applyItemPose(pose, entry.rotation());
                emitCapturedMesh(entry.itemMesh(), pose, emitter, entry.itemLight());
            }

            pose.popPose();
            pose.popPose();
        }
    }

    private static void emitCapturedMesh(
            ItemFrameItemModelBuilder.CapturedMesh mesh,
            PoseStack pose,
            MutableQuadViewImpl emitter,
            int light
    ) {
        for (ItemFrameItemModelBuilder.LayeredQuad layeredQuad : mesh.quads()) {
            SodiumChunkQuadEmitter.emitPackedQuad(
                    emitter,
                    pose.last(),
                    layeredQuad.quad(),
                    layeredQuad.color(),
                    light,
                    layeredQuad.layer()
            );
        }
    }

    private static void acceptLiveFrame(
            ItemFrame frame,
            LinkedHashMap<Integer, ItemFrame> framesById
    ) {
        ItemFrameRemovalTracker.markAdded(frame.getId());

        ItemFrameSectionRegistry.upsert(frame);

        MapId mapId = frame.getFramedMapId(frame.getItem());
        if (mapId != null && ItemFrameEligibility.isFrameMeshSupported(frame, mapId)) {
            ItemFrameMapIndex.upsert(frame, mapId, true);
        }

        framesById.put(frame.getId(), frame);
    }
}
