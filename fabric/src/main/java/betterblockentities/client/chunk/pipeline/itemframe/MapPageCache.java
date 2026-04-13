package betterblockentities.client.chunk.pipeline.itemframe;

/* local */
import betterblockentities.client.BBE;
import betterblockentities.client.chunk.pipeline.shelf.CacheKeys;
import betterblockentities.client.chunk.section.SectionUpdateDispatcher;
import betterblockentities.client.render.immediate.entity.ItemFrameRuntimeHelper;
import betterblockentities.client.tasks.TaskScheduler;
import betterblockentities.mixin.render.immediate.entity.SpriteContentsAccessor;

/* mojang */
import betterblockentities.mixin.render.immediate.entity.NativeImageAccessor;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

/* minecraft */
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.saveddata.maps.MapDecoration;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/* java */
import java.util.*;

/* fastutil */
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;

/* annotations */
import org.jspecify.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryUtil;

public final class MapPageCache {
    private static final long LIVE_INDEX_SEED_INTERVAL_TICKS = 20L;
    private static final long EMPTY_INDEX_SEED_INTERVAL_NANOS = 250_000_000L;

    private static final class PriorityAccumulator {
        private boolean visible;
        private double visibleDistanceSquared = Double.POSITIVE_INFINITY;
        private double anyDistanceSquared = Double.POSITIVE_INFINITY;

        private void offer(boolean visible, double distanceSquared) {
            this.anyDistanceSquared = Math.min(this.anyDistanceSquared, distanceSquared);
            if (!visible) return;

            this.visible = true;
            this.visibleDistanceSquared = Math.min(this.visibleDistanceSquared, distanceSquared);
        }

        private void reset() {
            this.visible = false;
            this.visibleDistanceSquared = Double.POSITIVE_INFINITY;
            this.anyDistanceSquared = Double.POSITIVE_INFINITY;
        }

        private boolean visible() { return this.visible; }

        private double distanceSquared() {
            return this.visible ? this.visibleDistanceSquared : this.anyDistanceSquared;
        }
    }

    private static final Object LOCK = new Object();
    private static final MapAtlasPool POOL = new MapAtlasPool();
    private static final IntOpenHashSet NORMAL_FALLBACK_MAP_IDS = new IntOpenHashSet();
    private static final IntOpenHashSet ACTIVE_MAP_IDS_SCRATCH = new IntOpenHashSet();
    private static final Int2ObjectOpenHashMap<PriorityAccumulator> PRIORITY_ACCUMULATORS = new Int2ObjectOpenHashMap<>();
    private static final ArrayList<MapId> DESIRED_ASSIGNMENT_SCRATCH = new ArrayList<>();
    private static MapId[] rankedMapIdsScratch = new MapId[0];
    private static boolean assignmentRefreshRequested;
    private static boolean[] rankedVisibleScratch = new boolean[0];
    private static boolean[] rankedAssignedScratch = new boolean[0];
    private static double[] rankedDistanceScratch = new double[0];

    private static final int MAX_DIRTY_MAP_UPLOADS_PER_REFRESH = 4;
    private static final IntOpenHashSet DIRTY_MAP_IDS = new IntOpenHashSet();
    private static final ArrayList<MapId> DIRTY_MAP_UPLOADS = new ArrayList<>();
    private static final int MAP_SIZE = MapAtlasBudgetPlanner.PAGE_SIZE;
    private static final int ICON_SIZE = 8;
    private static final int ROTATION_STEPS = 16;
    private static final int[] PACKED_MAP_COLORS = new int[256];
    private static final int[] PACKED_MAP_COLORS_NATIVE = new int[256];
    private static final double CAMERA_SECTION_GRACE_DISTANCE_SQUARED = 24.0D * 24.0D;
    private static long lastLiveIndexSeedTick = Long.MIN_VALUE;
    private static long lastEmptyIndexSeedNanos = Long.MIN_VALUE;
    private static volatile ItemFrameMapIndex.Entry[] VISIBLE_ENTRY_SNAPSHOT = new ItemFrameMapIndex.Entry[0];

    private static final HashMap<Identifier, DecorationIconCache> DECORATION_ICONS = new HashMap<>();
    private static volatile @Nullable TextureAtlas DECORATION_SPRITES;
    private static @Nullable NativeImage SCRATCH;
    private static @Nullable NativeImage PADDED_SCRATCH;

    private MapPageCache() {}

    private record VisibilitySnapshot(
            ItemFrameMapIndex.Entry[] visibleEntries,
            boolean[] visibleSections
    ) {}

    public static void setDecorationSprites(TextureAtlas decorationSprites) {
        TextureAtlas previous = DECORATION_SPRITES;
        DECORATION_SPRITES = decorationSprites;
        if (previous == decorationSprites) return;

        synchronized (LOCK) {
            DECORATION_ICONS.clear();
        }
    }

    public static @Nullable MapAtlasRef peekAtlasRef(MapId mapId) {
        MapItemSavedData currentMapData = currentMapData(mapId);

        synchronized (LOCK) {
            MapPage page = peekPage(mapId);
            if (page == null || !page.isReady()) return null;

            if (currentMapData != null && page.mapData() != currentMapData) {
                page.markStale();
            } else {
                return page.ref();
            }
        }

        ItemFrameSectionMapSurfaceRenderer.invalidateSnapshot();
        scheduleInitialUpload(mapId, currentMapData);
        return null;
    }

    public static @Nullable MapAtlasRef peekAtlasRefFast(MapId mapId) {
        MapAtlasManager.MapAtlasTexture atlas = MapAtlasManager.atlasNullable();
        if (atlas == null || !atlas.isAllocated()) return null;

        MapItemSavedData currentMapData = currentMapData(mapId);

        synchronized (LOCK) {
            MapPage page = POOL.peek(mapId);
            if (page == null || !page.isReady()) return null;

            if (currentMapData == null || page.mapData() == currentMapData) return page.ref();

            page.markStale();
        }

        ItemFrameSectionMapSurfaceRenderer.invalidateSnapshot();
        scheduleInitialUpload(mapId, currentMapData);
        return null;
    }

    public static int atlasAssignmentVersion() {synchronized (LOCK) { return POOL.assignmentVersion(); }}

    public static MapLifecycleState lifecycleForTrackedMap(MapId mapId, @Nullable MapItemSavedData mapData) {
        return lifecycleFor(mapId, mapData, true);
    }

    public static MapLifecycleState lifecycleForCapture(MapId mapId) {
        return lifecycleFor(mapId, null, false);
    }

    private static MapLifecycleState lifecycleFor(
            MapId mapId,
            @Nullable MapItemSavedData mapData,
            boolean scheduleUploads
    ) {
        boolean scheduleUpload = false;
        MapLifecycleState state;

        synchronized (LOCK) {
            boolean atlasReady = atlasReadyLocked();
            MapPage page = atlasReady ? POOL.peek(mapId) : null;
            if (page != null) {
                if (page.isReady()) {
                    state = MapLifecycleState.MESHED_READY;
                } else {
                    scheduleUpload = scheduleUploads && !page.uploadQueued();
                    state = MapLifecycleState.PENDING_DATA;
                }
            } else if (!atlasReady) {
                state = MapLifecycleState.NORMAL_FALLBACK;
            } else if (NORMAL_FALLBACK_MAP_IDS.contains(mapId.id())) {
                state = MapLifecycleState.NORMAL_FALLBACK;
            } else {
                // Atlas exists, but this map has no assigned page yet.
                // Without forcing assignment refresh, this can stay PENDING_DATA forever.
                if (scheduleUploads) assignmentRefreshRequested = true;

                state = MapLifecycleState.PENDING_DATA;
            }
        }

        if (scheduleUpload) scheduleInitialUpload(mapId, mapData);

        return state;
    }

    public static void onMapDataUpdated(MapId mapId) {
        synchronized (LOCK) {
            MapPage page = peekPage(mapId);
            if (page == null) return;

            if (DIRTY_MAP_IDS.add(mapId.id())) {
                DIRTY_MAP_UPLOADS.add(mapId);
            }
        }
    }

    static {
        for (int i = 0; i < 256; i++) {
            int argb = MapColor.getColorFromPackedId(i);
            PACKED_MAP_COLORS[i] = argb;
            PACKED_MAP_COLORS_NATIVE[i] = argbToAbgr(argb);
        }
    }

    private static int argbToAbgr(int argb) {
        return (argb & 0xFF00FF00)
                | ((argb & 0x00FF0000) >> 16)
                | ((argb & 0x000000FF) << 16);
    }

    public static void invalidateAllCachesOnReload() {
        synchronized (LOCK) {
            DIRTY_MAP_IDS.clear();
            DIRTY_MAP_UPLOADS.clear();
            DECORATION_ICONS.clear();
            closeScratch();
            NORMAL_FALLBACK_MAP_IDS.clear();
            POOL.invalidate();
            lastLiveIndexSeedTick = Long.MIN_VALUE;
            lastEmptyIndexSeedNanos = Long.MIN_VALUE;
            assignmentRefreshRequested = false;
            VISIBLE_ENTRY_SNAPSHOT = new ItemFrameMapIndex.Entry[0];
            clearAssignmentScratch();
        }
    }

    public static void invalidateRuntimeStateOnLevelChange() {
        synchronized (LOCK) {
            DIRTY_MAP_IDS.clear();
            DIRTY_MAP_UPLOADS.clear();
            NORMAL_FALLBACK_MAP_IDS.clear();
            POOL.invalidate();
            lastLiveIndexSeedTick = Long.MIN_VALUE;
            lastEmptyIndexSeedNanos = Long.MIN_VALUE;
            assignmentRefreshRequested = false;
            VISIBLE_ENTRY_SNAPSHOT = new ItemFrameMapIndex.Entry[0];
            clearAssignmentScratch();
        }
    }

    public static void refreshVisibleAssignments(Vec3 cameraPos) {
        Collection<MapId> assignedMapIdsToUpload = List.of();
        Collection<MapId> changedMapIds = List.of();
        ClientLevel level = Minecraft.getInstance().level;

        flushDirtyMapUploads(level);

        // This is internally throttled. It keeps render-only/replay frames recoverable
        // without forcing every section capture to scan live entities. If the index is
        // completely empty, keep probing briefly even after bootstrap says it is primed.
        if (shouldSeedEmptyMapIndex(level)) {
            seedMapIndexFromLiveFrames(level, true);
        } else if (ItemFrameRuntimeHelper.shouldRunLiveFrameRecoverySeed()) {
            seedMapIndexFromLiveFrames(level, false);
        }

        MapId[] activeMapIdSnapshot = ItemFrameMapIndex.activeMapIdSnapshot();
        MapAtlasBudgetPlanner.BudgetResult budget = MapAtlasManager.budgetNullable();
        int safeBudget = budget != null ? budget.safeBudget() : 0;

        ItemFrameMapIndex.Entry[] entrySnapshot = ItemFrameMapIndex.entrySnapshot();
        ItemFrameMapIndex.SectionBucket[] sectionBucketSnapshot = ItemFrameMapIndex.sectionBucketSnapshot();

        VisibilitySnapshot visibilitySnapshot = captureVisibleEntries(
                sectionBucketSnapshot,
                entrySnapshot.length,
                cameraPos,
                BBE.GlobalScope.frustum
        );

        updateVisibleEntrySnapshot(visibilitySnapshot.visibleEntries());

        IntOpenHashSet activeMapIds = ACTIVE_MAP_IDS_SCRATCH;
        activeMapIds.clear();

        for (MapId mapId : activeMapIdSnapshot) activeMapIds.add(mapId.id());

        synchronized (LOCK) {
            if (!atlasReadyLocked()) {
                NORMAL_FALLBACK_MAP_IDS.clear();
                return;
            }

            boolean refreshAssignments = assignmentRefreshRequested
                    || POOL.needsAssignmentRefresh(activeMapIds, activeMapIdSnapshot.length);
            assignmentRefreshRequested = false;

            if (refreshAssignments) {
                List<MapId> desiredMapIds = collectDesiredMapAssignments(
                        activeMapIdSnapshot,
                        activeMapIds,
                        sectionBucketSnapshot,
                        visibilitySnapshot.visibleSections(),
                        cameraPos,
                        safeBudget
                );

                MapAtlasPool.AssignmentDelta delta = POOL.syncAssignments(desiredMapIds, activeMapIds);

                HashSet<MapId> changed = new HashSet<>(
                        delta.assigned().size() + delta.released().size()
                );
                changed.addAll(delta.assigned());
                changed.addAll(delta.released());

                changedMapIds = changed;
                assignedMapIdsToUpload = new ArrayList<>(delta.assigned());
            }

            NORMAL_FALLBACK_MAP_IDS.clear();
            for (MapId mapId : activeMapIdSnapshot) if (!POOL.isAssigned(mapId)) NORMAL_FALLBACK_MAP_IDS.add(mapId.id());
        }

        for (MapId mapId : assignedMapIdsToUpload) {
            scheduleInitialUpload(mapId, level != null ? level.getMapData(mapId) : null);
        }

        for (MapId mapId : changedMapIds) scheduleSectionRebuilds(mapId);
    }

    private static @Nullable MapPage peekPage(MapId mapId) {
        if (!atlasReadyLocked()) return null;

        return POOL.peek(mapId);
    }

    private static boolean atlasReadyLocked() {
        MapAtlasManager.MapAtlasTexture atlas = MapAtlasManager.atlasNullable();
        if (atlas == null || !atlas.isAllocated()) return false;

        MapAtlasBudgetPlanner.BudgetResult budget = MapAtlasManager.budgetNullable();
        if (budget == null || budget.safeBudget() <= 0) return false;

        return !POOL.ensureInitialized();
    }

    private static void scheduleInitialUpload(MapId mapId, @Nullable MapItemSavedData initialMapData) {
        int levelEpoch = ItemFrameRuntimeHelper.captureLevelEpoch();

        synchronized (LOCK) {
            MapPage page = peekPage(mapId);
            if (page == null || page.isReady() || page.uploadQueued()) return;

            page.uploadQueued(true);
        }

        TaskScheduler.schedule(() -> {
            if (!ItemFrameRuntimeHelper.isLevelEpochCurrent(levelEpoch)) return;

            boolean uploaded;

            synchronized (LOCK) {
                if (!ItemFrameRuntimeHelper.isLevelEpochCurrent(levelEpoch)) return;

                MapPage page = peekPage(mapId);
                if (page == null) return;

                if (page.isReady()) {
                    page.uploadQueued(false);
                    return;
                }

                MapItemSavedData mapData = currentMapData(mapId);
                if (mapData == null) mapData = initialMapData;

                if (mapData == null) {
                    page.uploadQueued(false);
                    return;
                }

                uploadPageIfNeeded(page, mapData);
                page.uploadQueued(false);
                uploaded = page.isReady();
            }

            if (uploaded) scheduleSectionRebuilds(mapId);
        });
    }

    private static void uploadPageIfNeeded(MapPage page, @Nullable MapItemSavedData mapData) {
        if (mapData == null) return;

        long contentHash = computeContentHash(mapData);
        if (page.mapData() == mapData && page.contentHash() == contentHash) return;

        MapAtlasManager.MapAtlasTexture atlas = MapAtlasManager.atlasNullable();
        if (atlas == null || !atlas.isAllocated()) return;

        boolean wasReady = page.isReady();
        NativeImage pageImage = scratchImage();
        rasterizeMap(pageImage, mapData);

        uploadPageImage(page, pageImage, atlas.getTexture());
        page.markUploaded(mapData, contentHash);

        if (!wasReady) ItemFrameSectionMapSurfaceRenderer.invalidateSnapshot();
    }

    private static void rasterizeMap(NativeImage target, MapItemSavedData mapData) {
        writeBaseMapPixelsFast(target, mapData.colors);

        for (MapDecoration decoration : mapData.getDecorations()) {
            if (!decoration.renderOnFrame()) continue;

            DecorationIconCache iconCache = getDecorationIconCache(decoration.getSpriteLocation());
            if (iconCache != null) drawDecoration(target, iconCache.pixels(), decoration);
        }
    }

    private static void writeBaseMapPixelsFast(NativeImage target, byte[] colors) {
        long pixels = nativePixels(target);

        if (pixels == 0L) {
            writeBaseMapPixelsSlow(target, colors);
            return;
        }

        for (int i = 0; i < MAP_SIZE * MAP_SIZE; i++) {
            MemoryUtil.memPutInt(
                    pixels + (((long) i) << 2),
                    PACKED_MAP_COLORS_NATIVE[colors[i] & 0xFF]
            );
        }
    }

    @SuppressWarnings({"DataFlowIssue", "CastToIncompatibleInterface"})
    private static long nativePixels(NativeImage target) { return ((NativeImageAccessor) (Object) target).getPixels(); }

    private static void writeBaseMapPixelsSlow(NativeImage target, byte[] colors) {
        for (int y = 0; y < MAP_SIZE; y++) {
            int row = y * MAP_SIZE;

            for (int x = 0; x < MAP_SIZE; x++) {
                int index = row + x;
                target.setPixel(x, y, PACKED_MAP_COLORS[colors[index] & 0xFF]);
            }
        }
    }

    private static void drawDecoration(NativeImage target, int[] iconPixels, MapDecoration decoration) {
        PoseStack poseStack = new PoseStack();
        poseStack.translate(decoration.x() / 2.0F + 64.0F, decoration.y() / 2.0F + 64.0F, 0.0F);
        poseStack.mulPose(Axis.ZP.rotationDegrees(decoration.rot() * 360.0F / ROTATION_STEPS));
        poseStack.scale(4.0F, 4.0F, 3.0F);
        poseStack.translate(-0.125F, 0.125F, 0.0F);

        Matrix4f pose = poseStack.last().pose();
        Vector3f transformed = new Vector3f();

        for (int sourceY = 0; sourceY < ICON_SIZE; sourceY++) {
            int row = sourceY * ICON_SIZE;
            for (int sourceX = 0; sourceX < ICON_SIZE; sourceX++) {
                int source = iconPixels[row + sourceX];
                if (ARGB.alpha(source) == 0) continue;

                float localX = -1.0F + (sourceX + 0.5F) * (2.0F / ICON_SIZE);
                float localY = 1.0F - (sourceY + 0.5F) * (2.0F / ICON_SIZE);
                pose.transformPosition(localX, localY, 0.0F, transformed);

                int targetX = Mth.floor(transformed.x());
                int targetY = Mth.floor(transformed.y());
                if (targetX < 0 || targetX >= MAP_SIZE || targetY < 0 || targetY >= MAP_SIZE) continue;

                int background = target.getPixel(targetX, targetY);
                target.setPixel(targetX, targetY, alphaBlend(background, source));
            }
        }
    }

    private static int alphaBlend(int background, int foreground) {
        int fgA = ARGB.alpha(foreground);
        if (fgA == 255) return foreground;
        if (fgA == 0) return background;

        int bgA = ARGB.alpha(background);
        int outA = fgA + bgA * (255 - fgA) / 255;
        if (outA <= 0) return 0;

        int outR = (ARGB.red(foreground) * fgA + ARGB.red(background) * bgA * (255 - fgA) / 255) / outA;
        int outG = (ARGB.green(foreground) * fgA + ARGB.green(background) * bgA * (255 - fgA) / 255) / outA;
        int outB = (ARGB.blue(foreground) * fgA + ARGB.blue(background) * bgA * (255 - fgA) / 255) / outA;
        return ARGB.color(outA, outR, outG, outB);
    }

    private static @Nullable DecorationIconCache getDecorationIconCache(Identifier spriteId) {
        DecorationIconCache cache = DECORATION_ICONS.get(spriteId);
        if (cache == null) {
            cache = loadDecorationIconCache(spriteId);
            if (cache == null) return null;

            DECORATION_ICONS.put(spriteId, cache);
        }

        return cache;
    }

    private static @Nullable DecorationIconCache loadDecorationIconCache(Identifier spriteId) {
        TextureAtlas decorationSprites = DECORATION_SPRITES;
        if (decorationSprites == null) return null;

        TextureAtlasSprite sprite = decorationSprites.getSprite(spriteId);
        SpriteContents contents = sprite.contents();
        NativeImage source = ((SpriteContentsAccessor) contents).getOriginalImage();
        int width = contents.width();
        int height = contents.height();

        if (source == null || width <= 0 || height <= 0) return null;

        if (width == ICON_SIZE && height == ICON_SIZE) {
            return new DecorationIconCache(readPixels(source));
        }

        try (NativeImage resized = new NativeImage(ICON_SIZE, ICON_SIZE, true)) {
            source.resizeSubRectTo(0, 0, width, height, resized);
            return new DecorationIconCache(readPixels(resized));
        }
    }

    private static int[] readPixels(NativeImage image) {
        int[] pixels = new int[ICON_SIZE * ICON_SIZE];

        for (int y = 0; y < ICON_SIZE; y++) {
            int row = y * ICON_SIZE;
            for (int x = 0; x < ICON_SIZE; x++) {
                pixels[row + x] = image.getPixel(x, y);
            }
        }

        return pixels;
    }

    private static void uploadPageImage(MapPage page, NativeImage image, GpuTexture texture) {
        MapAtlasRef ref = page.ref();
        NativeImage padded = paddedScratchImage();
        int padding = MapAtlasBudgetPlanner.PAGE_GUTTER;
        int uploadSize = MapAtlasBudgetPlanner.SLOT_STRIDE;

        for (int y = 0; y < uploadSize; y++) {
            int sourceY = Mth.clamp(y - padding, 0, image.getHeight() - 1);
            for (int x = 0; x < uploadSize; x++) {
                int sourceX = Mth.clamp(x - padding, 0, image.getWidth() - 1);
                padded.setPixel(x, y, image.getPixel(sourceX, sourceY));
            }
        }

        RenderSystem.getDevice()
                .createCommandEncoder()
                .writeToTexture(
                        texture,
                        padded,
                        0,
                        0,
                        ref.slotX(),
                        ref.slotY(),
                        uploadSize,
                        uploadSize,
                        0,
                        0
                );
    }

    private static long computeContentHash(@Nullable MapItemSavedData mapData) {
        if (mapData == null) return 0L;

        long signature = 0xCBF29CE484222325L;
        for (byte color : mapData.colors) {
            signature ^= color & 0xFFL;
            signature *= 0x100000001B3L;
        }
        int decorationCount = 0;

        for (MapDecoration decoration : mapData.getDecorations()) {
            if (!decoration.renderOnFrame()) continue;

            long decorationSignature = decoration.getSpriteLocation().hashCode();
            decorationSignature = CacheKeys.mix64(decorationSignature, decoration.x());
            decorationSignature = CacheKeys.mix64(decorationSignature, decoration.y());
            decorationSignature = CacheKeys.mix64(decorationSignature, decoration.rot());
            decorationSignature = CacheKeys.mix64(decorationSignature, 1L);

            signature = CacheKeys.mix64(signature, decorationSignature);
            decorationCount++;
        }

        long contentHash = CacheKeys.mix64(signature, decorationCount);
        return contentHash != Long.MIN_VALUE ? contentHash : Long.MAX_VALUE;
    }

    private static @Nullable MapItemSavedData currentMapData(MapId mapId) {
        ClientLevel level = Minecraft.getInstance().level;
        return level != null ? level.getMapData(mapId) : null;
    }

    private static void scheduleSectionRebuilds(MapId mapId) {
        Long2ObjectOpenHashMap<BlockPos> targets = new Long2ObjectOpenHashMap<>();

        for (ItemFrameMapIndex.Entry entry : ItemFrameMapIndex.entriesForMapSnapshot(mapId)) {
            BlockPos supportPos = entry.supportPos();
            targets.putIfAbsent(supportPos.asLong(), supportPos);
        }

        for (BlockPos target : targets.values()) SectionUpdateDispatcher.queueRebuildAtBlockPos(target);
    }

    public static int debugIndexedMapCount() { return ItemFrameMapIndex.activeMapIdSnapshot().length; }

    public static int debugAssignedMapCount() {synchronized (LOCK) { return POOL.assignedMapCount(); }}

    public static int debugReadyMapCount() {synchronized (LOCK) { return POOL.readyMapCount(); }}

    public static int debugFallbackMapCount() {synchronized (LOCK) { return NORMAL_FALLBACK_MAP_IDS.size(); }}

    public static ItemFrameMapIndex.Entry[] visibleEntrySnapshot() { return VISIBLE_ENTRY_SNAPSHOT; }

    private static void updateVisibleEntrySnapshot(ItemFrameMapIndex.Entry[] visibleEntries) {
        ItemFrameMapIndex.Entry[] current = VISIBLE_ENTRY_SNAPSHOT;
        if (sameVisibleEntries(current, visibleEntries)) return;

        VISIBLE_ENTRY_SNAPSHOT = visibleEntries;
    }

    private static boolean sameVisibleEntries(
            ItemFrameMapIndex.Entry[] current,
            ItemFrameMapIndex.Entry[] next
    ) {
        if (current == next) return true;
        if (current.length != next.length) return false;

        for (int i = 0; i < current.length; i++) {
            if (!current[i].equals(next[i])) return false;
        }

        return true;
    }

    private static List<MapId> collectDesiredMapAssignments(
            MapId[] activeMapIds,
            IntSet activeMapIdSet,
            ItemFrameMapIndex.SectionBucket[] sectionBucketSnapshot,
            boolean[] visibleSections,
            Vec3 cameraPos,
            int limit
    ) {
        if (limit <= 0 || activeMapIds.length == 0) {
            DESIRED_ASSIGNMENT_SCRATCH.clear();
            return DESIRED_ASSIGNMENT_SCRATCH;
        }

        ArrayList<MapId> desired = prepareDesiredAssignmentScratch(Math.min(limit, activeMapIds.length));

        if (limit >= activeMapIds.length) {
            Collections.addAll(desired, activeMapIds);
            return desired;
        }

        resetPriorityAccumulators();
        for (int i = 0; i < sectionBucketSnapshot.length; i++) {
            boolean visibleSection = visibleSections[i];
            for (ItemFrameMapIndex.Entry entry : sectionBucketSnapshot[i].entries()) {
                MapId candidateMapId = entry.mapId();
                int candidateMapNumericId = candidateMapId.id();
                if (!activeMapIdSet.contains(candidateMapNumericId)) continue;

                double distanceSquared = distanceSquaredForEntry(entry, cameraPos);
                PriorityAccumulator priority = PRIORITY_ACCUMULATORS.get(candidateMapNumericId);
                if (priority == null) {
                    priority = new PriorityAccumulator();
                    PRIORITY_ACCUMULATORS.put(candidateMapNumericId, priority);
                }

                priority.offer(visibleSection, distanceSquared);
            }
        }

        ensureRankedScratchCapacity(limit);
        int selectedCount = 0;
        boolean heapified = false;
        for (MapId activeMapId : activeMapIds) {
            PriorityAccumulator accumulator = PRIORITY_ACCUMULATORS.get(activeMapId.id());
            boolean visible = accumulator != null && accumulator.visible();
            double distanceSquared = accumulator != null ? accumulator.distanceSquared() : Double.POSITIVE_INFINITY;
            boolean assigned = POOL.isAssigned(activeMapId);

            if (selectedCount < limit) {
                putRankedScratch(selectedCount++, activeMapId, visible, assigned, distanceSquared);
                continue;
            }

            if (!heapified) {
                heapifyRankedScratch(selectedCount);
                heapified = true;
            }

            if (compareRankedValues(
                    visible,
                    assigned,
                    distanceSquared,
                    activeMapId.id(),
                    rankedVisibleScratch[0],
                    rankedAssignedScratch[0],
                    rankedDistanceScratch[0],
                    rankedMapIdsScratch[0].id()
            ) < 0) {
                putRankedScratch(0, activeMapId, visible, assigned, distanceSquared);
                siftRankedScratchDown(0, selectedCount);
            }
        }
        addRankedMapIdsToDesired(desired, selectedCount);

        return desired;
    }

    private static void addRankedMapIdsToDesired(ArrayList<MapId> desired, int desiredCount) {
        for (int i = 0; i < desiredCount; i++) {
            MapId mapId = rankedMapIdsScratch[i];
            if (mapId != null) desired.add(mapId);
        }
    }

    private static ArrayList<MapId> prepareDesiredAssignmentScratch(int capacity) {
        DESIRED_ASSIGNMENT_SCRATCH.clear();
        DESIRED_ASSIGNMENT_SCRATCH.ensureCapacity(capacity);
        return DESIRED_ASSIGNMENT_SCRATCH;
    }

    private static void resetPriorityAccumulators() {
        for (PriorityAccumulator accumulator : PRIORITY_ACCUMULATORS.values()) accumulator.reset();
    }

    private static void ensureRankedScratchCapacity(int capacity) {
        if (rankedMapIdsScratch.length >= capacity) return;

        int newCapacity = Math.max(16, rankedMapIdsScratch.length);
        while (newCapacity < capacity) newCapacity *= 2;

        rankedMapIdsScratch = Arrays.copyOf(rankedMapIdsScratch, newCapacity);
        rankedVisibleScratch = Arrays.copyOf(rankedVisibleScratch, newCapacity);
        rankedAssignedScratch = Arrays.copyOf(rankedAssignedScratch, newCapacity);
        rankedDistanceScratch = Arrays.copyOf(rankedDistanceScratch, newCapacity);
    }

    private static void putRankedScratch(
            int index,
            MapId mapId,
            boolean visible,
            boolean assigned,
            double distanceSquared
    ) {
        rankedMapIdsScratch[index] = mapId;
        rankedVisibleScratch[index] = visible;
        rankedAssignedScratch[index] = assigned;
        rankedDistanceScratch[index] = distanceSquared;
    }

    private static void heapifyRankedScratch(int size) {
        for (int i = (size >>> 1) - 1; i >= 0; i--) siftRankedScratchDown(i, size);
    }

    private static void siftRankedScratchDown(int index, int size) {
        while (true) {
            int left = (index << 1) + 1;
            if (left >= size) return;

            int worst = left;
            int right = left + 1;
            if (right < size && compareRankedScratch(right, left) > 0) worst = right;
            if (compareRankedScratch(worst, index) <= 0) return;

            swapRankedScratch(index, worst);
            index = worst;
        }
    }

    private static int compareRankedScratch(int left, int right) {
        return compareRankedValues(
                rankedVisibleScratch[left],
                rankedAssignedScratch[left],
                rankedDistanceScratch[left],
                rankedMapIdsScratch[left].id(),
                rankedVisibleScratch[right],
                rankedAssignedScratch[right],
                rankedDistanceScratch[right],
                rankedMapIdsScratch[right].id()
        );
    }

    private static int compareRankedValues(
            boolean leftVisible,
            boolean leftAssigned,
            double leftDistanceSquared,
            int leftMapId,
            boolean rightVisible,
            boolean rightAssigned,
            double rightDistanceSquared,
            int rightMapId
    ) {
        if (leftVisible != rightVisible) return leftVisible ? -1 : 1;
        if (leftAssigned != rightAssigned) return leftAssigned ? -1 : 1;

        int distanceCompare = Double.compare(leftDistanceSquared, rightDistanceSquared);
        if (distanceCompare != 0) return distanceCompare;

        return Integer.compare(leftMapId, rightMapId);
    }

    private static void swapRankedScratch(int left, int right) {
        if (left == right) return;

        MapId mapId = rankedMapIdsScratch[left];
        rankedMapIdsScratch[left] = rankedMapIdsScratch[right];
        rankedMapIdsScratch[right] = mapId;

        boolean visible = rankedVisibleScratch[left];
        rankedVisibleScratch[left] = rankedVisibleScratch[right];
        rankedVisibleScratch[right] = visible;

        boolean assigned = rankedAssignedScratch[left];
        rankedAssignedScratch[left] = rankedAssignedScratch[right];
        rankedAssignedScratch[right] = assigned;

        double distance = rankedDistanceScratch[left];
        rankedDistanceScratch[left] = rankedDistanceScratch[right];
        rankedDistanceScratch[right] = distance;
    }

    private static void clearAssignmentScratch() {
        ACTIVE_MAP_IDS_SCRATCH.clear();
        PRIORITY_ACCUMULATORS.clear();
        DESIRED_ASSIGNMENT_SCRATCH.clear();
        Arrays.fill(rankedMapIdsScratch, null);
    }

    private static VisibilitySnapshot captureVisibleEntries(
            ItemFrameMapIndex.SectionBucket[] sectionBucketSnapshot,
            int ignoredEntryCount,
            Vec3 cameraPos,
            @Nullable Frustum frustum
    ) {
        boolean[] visibleSections = new boolean[sectionBucketSnapshot.length];

        int maxEntriesFromBuckets = 0;
        for (ItemFrameMapIndex.SectionBucket sectionBucket : sectionBucketSnapshot) {
            maxEntriesFromBuckets += sectionBucket.entries().length;
        }

        ItemFrameMapIndex.Entry[] visibleEntries = new ItemFrameMapIndex.Entry[maxEntriesFromBuckets];
        int visibleCount = 0;

        for (int i = 0; i < sectionBucketSnapshot.length; i++) {
            ItemFrameMapIndex.SectionBucket sectionBucket = sectionBucketSnapshot[i];
            boolean visible = isSectionVisible(sectionBucket.sectionPos(), cameraPos, frustum);
            visibleSections[i] = visible;
            if (!visible) continue;

            for (ItemFrameMapIndex.Entry entry : sectionBucket.entries()) visibleEntries[visibleCount++] = entry;
        }

        return new VisibilitySnapshot(
                visibleCount == visibleEntries.length
                        ? visibleEntries
                        : Arrays.copyOf(visibleEntries, visibleCount),
                visibleSections
        );
    }

    private static double distanceSquaredForEntry(ItemFrameMapIndex.Entry entry, Vec3 cameraPos) {
        return entry.entityPos().distanceToSqr(cameraPos);
    }

    private static boolean isSectionVisible(
            SectionPos sectionPos,
            Vec3 cameraPos,
            Frustum frustum
    ) {
        double minX = sectionPos.minBlockX();
        double minY = sectionPos.minBlockY();
        double minZ = sectionPos.minBlockZ();
        double centerX = minX + 8.0D;
        double centerY = minY + 8.0D;
        double centerZ = minZ + 8.0D;

        double dx = centerX - cameraPos.x();
        double dy = centerY - cameraPos.y();
        double dz = centerZ - cameraPos.z();
        double distanceSquared = dx * dx + dy * dy + dz * dz;
        if (distanceSquared <= CAMERA_SECTION_GRACE_DISTANCE_SQUARED) return true;
        if (frustum == null) return true;

        return frustum.isVisible(new AABB(minX, minY, minZ, minX + 16.0D, minY + 16.0D, minZ + 16.0D));
    }

    private static boolean shouldSeedEmptyMapIndex(@Nullable ClientLevel level) {
        if (level == null || ItemFrameMapIndex.activeMapIdSnapshot().length != 0) return false;

        long now = System.nanoTime();
        if (lastEmptyIndexSeedNanos != Long.MIN_VALUE
                && now - lastEmptyIndexSeedNanos < EMPTY_INDEX_SEED_INTERVAL_NANOS) {
            return false;
        }

        lastEmptyIndexSeedNanos = now;
        return true;
    }

    private static void seedMapIndexFromLiveFrames(@Nullable ClientLevel level, boolean force) {
        if (level == null) return;

        long gameTime = level.getGameTime();
        if (!force
                && lastLiveIndexSeedTick != Long.MIN_VALUE
                && gameTime - lastLiveIndexSeedTick < LIVE_INDEX_SEED_INTERVAL_TICKS) {
            return;
        }

        lastLiveIndexSeedTick = gameTime;
        for (Entity entity : level.entitiesForRendering()) {
            if (!(entity instanceof ItemFrame frame) || frame.isRemoved()) continue;

            MapId mapId = frame.getFramedMapId(frame.getItem());
            if (mapId == null) continue;
            if (!ItemFrameEligibility.isFrameMeshSupported(frame, mapId)) continue;

            ItemFrameRemovalTracker.markAdded(frame.getId());
            ItemFrameMapIndex.upsert(frame, mapId, true);
            ItemFrameSectionRegistry.upsert(frame);
        }
    }

    private static NativeImage scratchImage() {
        NativeImage scratch = SCRATCH;
        if (scratch != null) return scratch;

        scratch = allocatePersistentImage(MAP_SIZE);
        SCRATCH = scratch;
        return scratch;
    }

    private static NativeImage paddedScratchImage() {
        NativeImage padded = PADDED_SCRATCH;
        if (padded != null) return padded;

        padded = allocatePersistentImage(MapAtlasBudgetPlanner.SLOT_STRIDE);
        PADDED_SCRATCH = padded;
        return padded;
    }

    private static void closeScratch() {
        NativeImage scratch = SCRATCH;
        SCRATCH = null;
        NativeImage padded = PADDED_SCRATCH;
        PADDED_SCRATCH = null;

        if (scratch != null) scratch.close();
        if (padded != null) padded.close();
    }

    private static NativeImage allocatePersistentImage(int size) { return new NativeImage(size, size, true); }

    private record DecorationIconCache(int[] basePixels) {

        private int[] pixels() { return this.basePixels; }
    }

    private static void flushDirtyMapUploads(@Nullable ClientLevel level) {
        if (level == null) return;

        int uploadedCount = 0;

        while (uploadedCount < MAX_DIRTY_MAP_UPLOADS_PER_REFRESH) {
            MapId mapId;

            synchronized (LOCK) {
                if (DIRTY_MAP_UPLOADS.isEmpty()) return;

                mapId = DIRTY_MAP_UPLOADS.removeFirst();
                DIRTY_MAP_IDS.remove(mapId.id());
            }

            boolean promoteToSectionSurface;

            synchronized (LOCK) {
                MapPage page = peekPage(mapId);
                if (page == null) continue;

                boolean wasReady = page.isReady();
                uploadPageIfNeeded(page, level.getMapData(mapId));
                if (page.isReady()) page.uploadQueued(false);

                promoteToSectionSurface = !wasReady && page.isReady();
            }

            uploadedCount++;

            if (promoteToSectionSurface) {
                scheduleSectionRebuilds(mapId);
            }
        }
    }
}
