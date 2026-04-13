package betterblockentities.client.render.immediate.entity;

/* local */
import betterblockentities.client.chunk.pipeline.itemframe.*;
import betterblockentities.client.chunk.section.SectionRebuildCallbacks;
import betterblockentities.client.chunk.section.SectionUpdateDispatcher;
import betterblockentities.client.render.immediate.entity.extensions.ItemFrameExt;
import betterblockentities.client.tasks.TaskScheduler;
import betterblockentities.mixin.sodium.render.RenderSectionManagerAccessor;
import betterblockentities.mixin.sodium.render.SodiumWorldRendererAccessor;

/* minecraft */
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientChunkCache;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.saveddata.maps.MapDecoration;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

/* sodium */
import net.caffeinemc.mods.sodium.client.render.SodiumWorldRenderer;
import net.caffeinemc.mods.sodium.client.render.chunk.RenderSection;
import net.caffeinemc.mods.sodium.client.render.chunk.RenderSectionManager;

/* java */
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/* annotations */
import org.jspecify.annotations.Nullable;

public final class ItemFrameRuntimeHelper {
    private static final int MAX_MISSING_PAYLOAD_RETRIES = 2;
    private static final int LEVEL_BOOTSTRAP_PASSES = 8;

    private static final int MAP_LABEL_CACHE_PAGE_BITS = 8;
    private static final int MAP_LABEL_CACHE_PAGE_SIZE = 1 << MAP_LABEL_CACHE_PAGE_BITS;
    private static final int MAP_LABEL_CACHE_PAGE_MASK = MAP_LABEL_CACHE_PAGE_SIZE - 1;
    private static final byte MAP_LABELS_ABSENT = 1;
    private static final byte MAP_LABELS_PRESENT = 2;
    private static byte[][] namedDecorationLabelsCache = new byte[16][];
    private static final int LIVE_SECTION_DISCOVERY_TICKS_AFTER_LOAD = 40;
    private static int liveSectionDiscoveryTicksRemaining;
    private static int bootstrapPassesRemaining;
    private static int levelEpoch;
    private static volatile boolean sectionRegistryPrimed;
    private static long currentLevelGameTime = Long.MIN_VALUE;
    private static final Set<Long> PENDING_MISSING_PAYLOAD_REBUILD_SECTIONS = ConcurrentHashMap.newKeySet();
    private static final ConcurrentHashMap<Long, Integer> MISSING_PAYLOAD_RETRY_ATTEMPTS_BY_SECTION =
            new ConcurrentHashMap<>();

    private record UploadProbe(
            ItemFrameSectionUploadRegistry.UploadedFrame uploadedFrame,
            boolean retryable
    ) {}

    private ItemFrameRuntimeHelper() {}

    public static void onAdded(ItemFrame frame) {
        ItemFrameRemovalTracker.markAdded(frame.getId());
        ItemFrameExt ext = ext(frame);
        BlockPos supportPos = supportPos(frame);
        refreshCachedItemState(frame, ext);
        invalidateUploadedStateRefresh(ext);
        clearLatchedTerrainSnapshot(ext);
        ext.lastSupportPos(supportPos);
        ext.mapLifecycleState(MapLifecycleState.NONE);

        if (!optimizationEnabled()) {
            ItemFrameMapIndex.remove(frame.getId());
            ItemFrameSectionRegistry.remove(frame.getId());
            setImmediateFallback(frame.getId(), ext, supportPos);
            return;
        }

        boolean meshEligible = ItemFrameEligibility.isFrameMeshSupported(frame, ext.cachedFramedMapId());
        if (!meshEligible) {
            ItemFrameMapIndex.remove(frame.getId());
            ItemFrameSectionRegistry.remove(frame.getId());
            setImmediateFallback(frame.getId(), ext, supportPos);
            return;
        }

        upsertMapIndexFromCachedState(frame, ext);
        ItemFrameSectionRegistry.upsert(frame, true);

        MapLifecycleState lifecycleState = syncMapLifecycleState(frame, ext);

        if (isMeshDesiredMapState(lifecycleState)) {
            setMapFallbackOrWait(frame, ext, supportPos, lifecycleState);
        }

        scheduleRebuild(frame, supportPos, supportPos);

        int scheduledLevelEpoch = levelEpoch;
        TaskScheduler.schedule(() -> {
            if (!isLevelEpochCurrent(scheduledLevelEpoch) || frame.isRemoved()) return;
            onFrameContentsChanged(frame);
        });
    }

    public static void onLevelSet(@Nullable ClientLevel level) {
        levelEpoch++;
        ItemFrameEligibility.invalidateRuntimeStateOnLevelChange();
        ItemFrameMapSurfaceRegistry.clear();
        clearNamedDecorationLabelCache();
        ItemFrameFallbackMapRenderStateCache.clear();
        ItemFrameSectionMapSurfaceRenderer.invalidateSnapshot();
        ItemFrameSectionRegistry.clear();
        ItemFrameRemovalTracker.clear();
        clearMissingPayloadRetryState();

        sectionRegistryPrimed = false;
        currentLevelGameTime = level != null ? level.getGameTime() : Long.MIN_VALUE;

        if (level == null || !optimizationEnabled()) {
            bootstrapPassesRemaining = 0;
            liveSectionDiscoveryTicksRemaining = 0;
            return;
        }

        bootstrapPassesRemaining = LEVEL_BOOTSTRAP_PASSES;
        liveSectionDiscoveryTicksRemaining = LIVE_SECTION_DISCOVERY_TICKS_AFTER_LOAD;
    }

    public static void onRemoved(ItemFrame frame) {
        ItemFrameRemovalTracker.markRemoved(frame.getId());
        ItemFrameExt ext = ext(frame);
        invalidateUploadedStateRefresh(ext);
        BlockPos oldSupport = ext.lastSupportPos();
        BlockPos currentSupport = supportPos(frame);
        ItemFrameMapIndex.remove(frame.getId());
        ItemFrameSectionRegistry.remove(frame.getId());
        deactivateUploadedMapSurface(frame.getId());
        ext.mapLifecycleState(MapLifecycleState.NONE);
        ext.terrainMeshReady(true);
        ext.terrainMeshActive(false);
        ext.renderImmediateWhileWaiting(false);
        ext.contentRenderMode(ItemFrameContentRenderMode.NONE);
        ext.missingPayloadRetryCount(0);

        clearLatchedTerrainSnapshot(ext);
        clearUploadedSectionState(oldSupport, currentSupport);
        clearMissingPayloadRetryState(oldSupport, currentSupport);

        queueRebuildTargets(targets(oldSupport, currentSupport), null);
    }

    public static void onFrameContentsChanged(ItemFrame frame) {
        if (frame.isRemoved()) {
            onRemoved(frame);
            return;
        }

        ItemFrameExt ext = ext(frame);

        ensureCachedItemState(frame, ext);

        MapId previousMapId = ext.cachedFramedMapId();
        int previousItemRawId = ext.cachedItemRawId();
        int previousComponentsHash = ext.cachedComponentsHash();
        boolean previousHadContent = previousMapId != null || previousItemRawId != 0;

        refreshCachedItemState(frame, ext);
        invalidateUploadedStateRefresh(ext);

        boolean sameContent =
                previousHadContent
                        && Objects.equals(previousMapId, ext.cachedFramedMapId())
                        && previousItemRawId == ext.cachedItemRawId()
                        && previousComponentsHash == ext.cachedComponentsHash();

        upsertMapIndexFromCachedState(frame, ext);
        upsertSectionRegistryFromCachedState(frame, ext);

        BlockPos supportPos = supportPos(frame);

        // If content identity did not change, this is usually rotation / visual-state churn.
        // Keep the old terrain visual alive until Sodium uploads the rebuilt section.
        updateForStateChange(frame, supportPos, supportPos, sameContent);
    }

    public static void onSupportPossiblyChanged(ItemFrame frame) {
        if (frame.isRemoved()) {
            onRemoved(frame);
            return;
        }

        ItemFrameExt ext = ext(frame);
        invalidateUploadedStateRefresh(ext);
        ensureCachedItemState(frame, ext);
        BlockPos oldSupport = ext.lastSupportPos();
        BlockPos newSupport = supportPos(frame);

        upsertMapIndexFromCachedState(frame, ext);
        upsertSectionRegistryFromCachedState(frame, ext);

        if (oldSupport.equals(newSupport)) return;
        updateForStateChange(frame, oldSupport, newSupport);
    }

    public static void onMapDataUpdated(MapId mapId) { onMapDataUpdated(mapId, true); }

    public static void onMapDataUpdated(MapId mapId, boolean textureDirty) {
        if (!optimizationEnabled()) return;
        clearNamedDecorationLabelCache(mapId);
        ItemFrameFallbackMapRenderStateCache.markDirty(mapId, textureDirty);
        MapPageCache.onMapDataUpdated(mapId);
    }

    public static void tickBootstrapPasses() {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            bootstrapPassesRemaining = 0;
            liveSectionDiscoveryTicksRemaining = 0;
            sectionRegistryPrimed = false;
            currentLevelGameTime = Long.MIN_VALUE;
            return;
        }

        currentLevelGameTime = level.getGameTime();

        if (liveSectionDiscoveryTicksRemaining > 0) liveSectionDiscoveryTicksRemaining--;
        if (bootstrapPassesRemaining <= 0 || !optimizationEnabled()) return;

        bootstrapPassesRemaining--;

        LinkedHashSet<Integer> seenFrameIds = new LinkedHashSet<>();
        for (Entity entity : level.entitiesForRendering()) {
            if (!(entity instanceof ItemFrame frame) || frame.isRemoved()) continue;
            if (!seenFrameIds.add(frame.getId())) continue;

            onFrameContentsChanged(frame);
        }

        if (bootstrapPassesRemaining <= 0) sectionRegistryPrimed = true;
    }

    public static void onResourceReloaded() {
        ItemFrameMapSurfaceRegistry.clear();
        clearNamedDecorationLabelCache();
        ItemFrameFallbackMapRenderStateCache.clear();
        ItemFrameSectionMapSurfaceRenderer.invalidateSnapshot();
        ItemFrameSectionRegistry.clear();
        clearMissingPayloadRetryState();

        sectionRegistryPrimed = false;

        if (!optimizationEnabled()) {
            liveSectionDiscoveryTicksRemaining = 0;
            return;
        }

        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            liveSectionDiscoveryTicksRemaining = 0;
            return;
        }

        liveSectionDiscoveryTicksRemaining = LIVE_SECTION_DISCOVERY_TICKS_AFTER_LOAD;

        LinkedHashSet<Integer> seenFrameIds = new LinkedHashSet<>();
        for (Entity entity : level.entitiesForRendering()) {
            if (!(entity instanceof ItemFrame frame) || frame.isRemoved()) continue;
            if (!seenFrameIds.add(frame.getId())) continue;

            onFrameContentsChanged(frame);
        }

        sectionRegistryPrimed = true;
    }

    public static void onSectionUploaded(ItemFrameSectionAppender appender) {
        if (appender == null || appender.entries().isEmpty()) return;

        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return;

        clearMissingPayloadRetryState(appender.sectionOrigin());

        LinkedHashSet<BlockPos> staleReadyMapSupports = new LinkedHashSet<>();

        for (ItemFrameSectionAppender.Entry entry : appender.entries()) {
            ItemFrame frame = findLiveItemFrame(level, entry.entityId());
            if (frame == null) continue;

            BlockPos currentSupport = supportPos(frame);
            if (!currentSupport.equals(entry.supportPos())) continue;

            ItemFrameExt ext = ext(frame);
            ensureCachedItemState(frame, ext);
            ensureRuntimeRegistration(frame, ext);

            if (cachedRenderSignature(frame, ext) != entry.renderSignature()) continue;

            MapId cachedMapId = cachedMapId(frame, ext);
            boolean mapFrame = cachedMapId != null || entry.mapId() != null;
            boolean readyMapSurface = mapFrame && hasReadyMeshedMapSurface(frame, ext);

            if (readyMapSurface) {
                // Even if the section payload was captured as NONE while the atlas page
                // was pending, the atlas surface is ready now.
                activateTerrainMapSurface(frame.getId(), ext, currentSupport, entry.mapLight());

                if (entry.contentRenderMode() != ItemFrameContentRenderMode.SECTION_MAP_SURFACE) {
                    staleReadyMapSupports.add(currentSupport);
                }

                continue;
            }

            if (mapFrame) {
                MapLifecycleState lifecycleState = syncMapLifecycleState(frame, ext);

                if (isMeshDesiredMapState(lifecycleState)) {
                    setMapFallbackOrWait(frame, ext, currentSupport, lifecycleState);
                } else {
                    setImmediateFallback(frame.getId(), ext, currentSupport);
                }

                continue;
            }

            if (requiresReadyMeshedMapSurface(frame, ext, entry.contentRenderMode())
                    && !hasReadyMeshedMapSurface(frame, ext)) {
                setImmediateFallback(frame.getId(), ext, currentSupport);
                continue;
            }

            if (isBlankMapTerrainPayload(frame, ext, entry.contentRenderMode())) {
                setImmediateFallback(frame.getId(), ext, currentSupport);
                continue;
            }

            activateTerrainMesh(frame.getId(), ext, currentSupport, entry.contentRenderMode());
        }

        for (BlockPos supportPos : staleReadyMapSupports) {
            SectionUpdateDispatcher.queueRebuildAtBlockPos(supportPos);
        }
    }

    public static void refreshUploadedState(ItemFrame frame) {
        refreshUploadedState(frame, false);
    }

    private static void refreshUploadedState(ItemFrame frame, boolean force) {
        if (!optimizationEnabled()) return;

        ItemFrameExt ext = ext(frame);
        long gameTime = currentGameTime(frame);
        if (!force && uploadedStateRefreshCurrent(frame, ext, gameTime)) return;

        ensureCachedItemState(frame, ext);
        BlockPos currentSupport = supportPos(frame);
        ensureRuntimeRegistration(frame, ext);
        markUploadedStateRefreshed(ext, currentSupport, gameTime);
        MapLifecycleState lifecycleState = syncMapLifecycleState(frame, ext);
        if (lifecycleState == MapLifecycleState.NORMAL_FALLBACK) clearLatchedTerrainSnapshot(ext);
        if (!optimizationEnabled()) {
            clearLatchedTerrainSnapshot(ext);
            setImmediateFallback(frame.getId(), ext, currentSupport);
            return;
        }

        if (supportBlockMissing(frame, currentSupport)) {
            if (cachedMapId(frame, ext) != null && hasReadyMeshedMapSurface(frame, ext)) {
                activateReadyMapSurfaceForMissingSupport(frame, ext, currentSupport);
                return;
            }

            setImmediateFallback(frame.getId(), ext, currentSupport);
            return;
        }

        UploadProbe probe = probeUploadedFrame(currentSupport, frame.getId());
        ItemFrameSectionUploadRegistry.UploadedFrame uploadedFrame = probe.uploadedFrame();
        boolean meshDesiredMapState = isMeshDesiredMapState(lifecycleState);

        if (uploadedFrame == null) {
            if (activateFromLatestSectionSnapshot(frame)) return;
            if (meshDesiredMapState) {
                if (lifecycleState == MapLifecycleState.MESHED_READY
                        && activateReadyMapSurfaceWhileWaiting(frame, ext, currentSupport)) {
                    maybeRetryMissingPayload(ext, currentSupport, probe);
                    return;
                }

                setMapFallbackOrWait(frame, ext, currentSupport, lifecycleState);
                maybeRetryMissingPayload(ext, currentSupport, probe);
                return;
            }

            if (ext.latchedTerrainSnapshot()) {
                activateLatchedTerrainVisual(frame.getId(), ext, currentSupport);
                maybeRetryMissingPayload(ext, currentSupport, probe);
                return;
            }

            setImmediateFallback(frame.getId(), ext, currentSupport);
            maybeRetryMissingPayload(ext, currentSupport, probe);
            return;
        }

        if (cachedRenderSignature(frame, ext) != uploadedFrame.renderSignature()) {
            if (activateFromLatestSectionSnapshot(frame)) return;
            if (meshDesiredMapState) {
                if (lifecycleState == MapLifecycleState.MESHED_READY
                        && activateReadyMapSurfaceWhileWaiting(frame, ext, currentSupport)) {
                    maybeRetryMissingPayload(
                            ext,
                            currentSupport,
                            new UploadProbe(uploadedFrame, true)
                    );
                    return;
                }

                setMapFallbackOrWait(frame, ext, currentSupport, lifecycleState);
                maybeRetryMissingPayload(
                        ext,
                        currentSupport,
                        new UploadProbe(uploadedFrame, true)
                );
                return;
            }

            if (ext.latchedTerrainSnapshot()) {
                activateLatchedTerrainVisual(frame.getId(), ext, currentSupport);
                maybeRetryMissingPayload(
                        ext,
                        currentSupport,
                        new UploadProbe(uploadedFrame, true)
                );
                return;
            }

            setImmediateFallback(frame.getId(), ext, currentSupport);
            maybeRetryMissingPayload(
                    ext,
                    currentSupport,
                    new UploadProbe(uploadedFrame, true)
            );
            return;
        }

        if (cachedMapId(frame, ext) != null && hasReadyMeshedMapSurface(frame, ext)) {
            activateTerrainMapSurface(frame.getId(), ext, currentSupport, uploadedFrame.mapLight());
            return;
        }

        if (requiresReadyMeshedMapSurface(frame, ext, uploadedFrame.contentRenderMode())
                && !hasReadyMeshedMapSurface(frame, ext)) {
            setImmediateFallback(frame.getId(), ext, currentSupport);
            maybeRetryMissingPayload(
                    ext,
                    currentSupport,
                    new UploadProbe(uploadedFrame, true)
            );
            return;
        }

        if (isBlankMapTerrainPayload(frame, ext, uploadedFrame.contentRenderMode())) {
            setImmediateFallback(frame.getId(), ext, currentSupport);
            maybeRetryMissingPayload(
                    ext,
                    currentSupport,
                    new UploadProbe(uploadedFrame, true)
            );
            return;
        }

        activateTerrainMesh(frame.getId(), ext, currentSupport, uploadedFrame.contentRenderMode());
    }

    private static void updateForStateChange(ItemFrame frame, BlockPos oldSupport, BlockPos newSupport) {
        updateForStateChange(frame, oldSupport, newSupport, false);
    }

    private static void updateForStateChange(
            ItemFrame frame,
            BlockPos oldSupport,
            BlockPos newSupport,
            boolean preserveExistingVisuals
    ) {
        ItemFrameExt ext = ext(frame);

        boolean newEligible = optimizationEnabled() && ItemFrameEligibility.isFrameMeshSupported(frame);
        boolean supportUnchanged = oldSupport.equals(newSupport);
        boolean hadTerrainVisual =
                ext.terrainMeshActive()
                        || ext.latchedTerrainSnapshot()
                        || ItemFrameMapSurfaceRegistry.get(frame.getId()) != null;

        boolean preserveUntilUpload =
                preserveExistingVisuals
                        && supportUnchanged
                        && hadTerrainVisual
                        && newEligible;

        boolean hadTerrain = hasTerrainUntilUpload(ext);
        boolean needsRebuild = !ext.terrainMeshReady() || hadTerrain || newEligible;
        MapLifecycleState lifecycleState = syncMapLifecycleState(frame, ext);

        if (!newEligible || !supportUnchanged || lifecycleState == MapLifecycleState.NORMAL_FALLBACK) {
            clearLatchedTerrainSnapshot(ext);
            preserveUntilUpload = false;
        }

        if (!needsRebuild) {
            setImmediateFallback(frame.getId(), ext, newSupport);
            return;
        }

        if (preserveUntilUpload) {
            // Keep the old uploaded/chunk visual owned by terrain until the new section upload lands.
            // This avoids immediate fallback fighting the old mesh during rotation.
            ext.terrainMeshReady(true);
            ext.terrainMeshActive(true);
            ext.renderImmediateWhileWaiting(false);
            ext.lastSupportPos(newSupport);

            if (ext.contentRenderMode() == ItemFrameContentRenderMode.NONE
                    && ext.latchedTerrainSnapshot()) {
                ext.contentRenderMode(ext.latchedContentRenderMode());
            }

            scheduleRebuild(frame, oldSupport, newSupport, true);
            return;
        }

        if (isMeshDesiredMapState(lifecycleState)) {
            setMapFallbackOrWait(frame, ext, newSupport, lifecycleState);
        }

        scheduleRebuild(frame, oldSupport, newSupport, false);
    }

    private static void scheduleRebuild(
            ItemFrame frame,
            BlockPos oldSupport,
            BlockPos newSupport
    ) {
        scheduleRebuild(frame, oldSupport, newSupport, false);
    }

    private static void scheduleRebuild(
            ItemFrame frame,
            BlockPos oldSupport,
            BlockPos newSupport,
            boolean preserveExistingVisuals
    ) {
        ItemFrameExt ext = ext(frame);

        if (!preserveExistingVisuals) {
            ext.terrainMeshReady(false);
            ext.terrainMeshActive(false);
            ext.contentRenderMode(ItemFrameContentRenderMode.NONE);
            deactivateUploadedMapSurface(frame.getId());
        } else {
            // Keep the previous terrain/map visual active until the rebuild callback resolves.
            ext.terrainMeshReady(true);
            ext.terrainMeshActive(true);
            ext.renderImmediateWhileWaiting(false);

            if (ext.contentRenderMode() == ItemFrameContentRenderMode.NONE
                    && ext.latchedTerrainSnapshot()) {
                ext.contentRenderMode(ext.latchedContentRenderMode());
            }
        }

        invalidateUploadedStateRefresh(ext);
        ext.lastSupportPos(newSupport);

        Set<BlockPos> targets = targets(oldSupport, newSupport);
        ext.missingPayloadRetryCount(0);
        clearMissingPayloadRetryState(targets);

        int scheduledLevelEpoch = levelEpoch;
        queueRebuildTargets(targets, () -> {
            if (!isLevelEpochCurrent(scheduledLevelEpoch) || frame.isRemoved()) return;

            refreshUploadedState(frame, true);

            if (!ext.terrainMeshReady()) {
                activateFromLatestSectionSnapshot(frame);
            }
        });
    }

    private static boolean optimizationEnabled() { return ItemFrameEligibility.optimizationEnabled(); }

    public static int captureLevelEpoch() { return levelEpoch; }

    public static boolean isLevelEpochCurrent(int capturedLevelEpoch) { return capturedLevelEpoch == levelEpoch; }

    private static boolean hasReadyMeshedMapSurface(ItemFrame frame, ItemFrameExt ext) {
        MapId mapId = cachedMapId(frame, ext);
        return mapId != null && MapPageCache.peekAtlasRef(mapId) != null;
    }

    private static boolean isBlankMapTerrainPayload(
            ItemFrame frame,
            ItemFrameExt ext,
            ItemFrameContentRenderMode contentRenderMode
    ) {
        return contentRenderMode == ItemFrameContentRenderMode.NONE && cachedMapId(frame, ext) != null;
    }

    private static boolean requiresReadyMeshedMapSurface(
            ItemFrame frame,
            ItemFrameExt ext,
            ItemFrameContentRenderMode contentRenderMode
    ) {
        return contentRenderMode == ItemFrameContentRenderMode.SECTION_MAP_SURFACE
                && cachedMapId(frame, ext) != null;
    }

    private static void setMapFallbackOrWait(
            ItemFrame frame,
            ItemFrameExt ext,
            BlockPos supportPos,
            MapLifecycleState lifecycleState
    ) {
        if (cachedMapId(frame, ext) == null) {
            setPendingMeshWait(frame.getId(), ext, supportPos);
            return;
        }

        int retryCount = ext.missingPayloadRetryCount();

        switch (lifecycleState) {
            case PENDING_DATA -> {
                // Keep newly-uploading maps visible until the atlas surface exists, but do not
                // reset the retry budget on every refresh, or they will thrash section rebuilds.
                ext.terrainMeshReady(false);
                ext.terrainMeshActive(false);
                ext.renderImmediateWhileWaiting(true);
            }
            case MESHED_READY -> {
                if (activateReadyMapSurfaceWhileWaiting(frame, ext, supportPos)) return;

                setPendingMeshWait(frame.getId(), ext, supportPos);
                return;
            }
            case NORMAL_FALLBACK, NONE -> {
                setImmediateFallback(frame.getId(), ext, supportPos);
                return;
            }
        }

        ext.lastSupportPos(supportPos);
        ext.missingPayloadRetryCount(retryCount);
        ext.contentRenderMode(ItemFrameContentRenderMode.NONE);
        deactivateUploadedMapSurface(frame.getId());
    }

    private static MapLifecycleState syncMapLifecycleState(ItemFrame frame, ItemFrameExt ext) {
        ensureCachedItemState(frame, ext);
        MapLifecycleState previousState = ext.mapLifecycleState();
        MapId mapId = ext.cachedFramedMapId();
        if (mapId == null || !ItemFrameEligibility.isFrameMeshSupported(frame, mapId)) {
            ext.mapLifecycleState(MapLifecycleState.NONE);
            return MapLifecycleState.NONE;
        }

        MapLifecycleState lifecycleState = MapPageCache.lifecycleForTrackedMap(
                mapId,
                frame.level().getMapData(mapId)
        );
        ext.mapLifecycleState(lifecycleState);
        if (previousState != MapLifecycleState.MESHED_READY
                && lifecycleState == MapLifecycleState.MESHED_READY) {
            // If a visible map only becomes atlas-ready after earlier missing-payload retries
            // were already exhausted, force a fresh section capture now that the surface exists.
            ext.missingPayloadRetryCount(0);
            SectionUpdateDispatcher.queueRebuildAtBlockPos(supportPos(frame));
        }
        return lifecycleState;
    }

    private static boolean isMeshDesiredMapState(MapLifecycleState lifecycleState) {
        return lifecycleState == MapLifecycleState.PENDING_DATA
                || lifecycleState == MapLifecycleState.MESHED_READY;
    }

    private static boolean hasTerrainUntilUpload(ItemFrameExt ext) {
        return ext.terrainMeshActive()
                || (!ext.terrainMeshReady() && !ext.renderImmediateWhileWaiting());
    }

    private static void setImmediateFallback(
            int entityId,
            ItemFrameExt ext,
            BlockPos supportPos
    ) {
        ext.terrainMeshReady(true);
        ext.terrainMeshActive(false);
        ext.renderImmediateWhileWaiting(true);
        ext.lastSupportPos(supportPos);
        ext.contentRenderMode(ItemFrameContentRenderMode.NONE);
        deactivateUploadedMapSurface(entityId);
    }

    private static void setPendingMeshWait(
            int entityId,
            ItemFrameExt ext,
            BlockPos supportPos
    ) {
        ext.terrainMeshReady(false);
        ext.terrainMeshActive(false);
        ext.renderImmediateWhileWaiting(false);
        ext.lastSupportPos(supportPos);
        ext.missingPayloadRetryCount(0);
        ext.contentRenderMode(ItemFrameContentRenderMode.NONE);
        deactivateUploadedMapSurface(entityId);
    }

    private static void activateTerrainMesh(
            int entityId,
            ItemFrameExt ext,
            BlockPos supportPos,
            ItemFrameContentRenderMode contentRenderMode
    ) {
        ext.terrainMeshReady(true);
        ext.terrainMeshActive(true);
        ext.renderImmediateWhileWaiting(false);
        ext.lastSupportPos(supportPos);
        ext.missingPayloadRetryCount(0);
        ext.contentRenderMode(contentRenderMode);
        if (contentRenderMode != ItemFrameContentRenderMode.SECTION_MAP_SURFACE) {
            deactivateUploadedMapSurface(entityId);
        }

        ext.latchedTerrainSnapshot(true);
        ext.latchedContentRenderMode(contentRenderMode);
    }

    private static void maybeRetryMissingPayload(
            ItemFrameExt ext,
            BlockPos supportPos,
            UploadProbe probe
    ) {
        if (!probe.retryable()) return;

        int retries = ext.missingPayloadRetryCount();
        if (retries >= MAX_MISSING_PAYLOAD_RETRIES) return;
        if (!queueMissingPayloadRetryRebuild(supportPos)) return;

        ext.missingPayloadRetryCount(retries + 1);
    }

    private static boolean queueMissingPayloadRetryRebuild(BlockPos supportPos) {
        long key = SectionRebuildCallbacks.keyFromBlockPos(supportPos);
        if (!PENDING_MISSING_PAYLOAD_REBUILD_SECTIONS.add(key)) {
            return false;
        }

        int attempts = MISSING_PAYLOAD_RETRY_ATTEMPTS_BY_SECTION.merge(key, 1, Integer::sum);
        if (attempts > MAX_MISSING_PAYLOAD_RETRIES) {
            PENDING_MISSING_PAYLOAD_REBUILD_SECTIONS.remove(key);
            return false;
        }

        SectionUpdateDispatcher.queueRebuildAtBlockPos(
                supportPos,
                () -> PENDING_MISSING_PAYLOAD_REBUILD_SECTIONS.remove(key)
        );
        return true;
    }

    private static boolean activateFromLatestSectionSnapshot(ItemFrame frame) {
        ItemFrameExt ext = ext(frame);
        ensureCachedItemState(frame, ext);
        BlockPos currentSupport = supportPos(frame);
        MapId currentMapId = ext.cachedFramedMapId();
        ItemFrameSectionAppender appender = ItemFrameSectionBuildBridge.latest(SectionPos.of(currentSupport));
        if (appender == null) {
            return false;
        }

        long currentSignature = cachedRenderSignature(frame, ext);
        for (ItemFrameSectionAppender.Entry entry : appender.entries()) {
            if (entry.entityId() != frame.getId()) continue;
            if (!currentSupport.equals(entry.supportPos())) continue;
            if (entry.renderSignature() != currentSignature) continue;

            if (currentMapId != null) {
                if (!currentMapId.equals(entry.mapId()) || !hasReadyMeshedMapSurface(frame, ext)) {
                    continue;
                }

                activateTerrainMapSurface(frame.getId(), ext, currentSupport, entry.mapLight());
                return true;
            }

            if (requiresReadyMeshedMapSurface(frame, ext, entry.contentRenderMode())
                    && !hasReadyMeshedMapSurface(frame, ext)) continue;
            if (isBlankMapTerrainPayload(frame, ext, entry.contentRenderMode())) continue;

            if (entry.contentRenderMode() == ItemFrameContentRenderMode.SECTION_MAP_SURFACE) {
                activateTerrainMapSurface(frame.getId(), ext, currentSupport, entry.mapLight());
            } else {
                activateTerrainMesh(frame.getId(), ext, currentSupport, entry.contentRenderMode());
            }
            return true;
        }

        return false;
    }

    private static UploadProbe probeUploadedFrame(BlockPos supportPos, int entityId) {
        ItemFrameSectionUploadRegistry.UploadedFrame uploadedFrame =
                ItemFrameSectionUploadRegistry.getFrame(supportPos, entityId);
        if (uploadedFrame != null) {
            return new UploadProbe(uploadedFrame, false);
        }

        SodiumWorldRenderer renderer = SodiumWorldRenderer.instanceNullable();
        if (renderer == null) {
            return new UploadProbe(null, false);
        }

        RenderSectionManager manager = ((SodiumWorldRendererAccessor) renderer).getRenderSectionManager();
        if (manager == null) {
            return new UploadProbe(null, false);
        }

        RenderSection section = ((RenderSectionManagerAccessor) manager).invokeGetRenderSection(
                SectionPos.blockToSectionCoord(supportPos.getX()),
                SectionPos.blockToSectionCoord(supportPos.getY()),
                SectionPos.blockToSectionCoord(supportPos.getZ())
        );
        if (section == null) {
            return new UploadProbe(null, false);
        }

        ItemFrameSectionAppender appender = ((RenderSectionItemFrameExt) section).getItemFrameSectionAppender();
        if (appender == null) {
            return new UploadProbe(null, true);
        }

        for (ItemFrameSectionAppender.Entry entry : appender.entries()) {

            if (entry.entityId() == entityId && supportPos.equals(entry.supportPos())) {
                return new UploadProbe(
                        new ItemFrameSectionUploadRegistry.UploadedFrame(
                                entry.renderSignature(),
                                entry.contentRenderMode(),
                                entry.mapLight()
                        ),
                        false
                );
            }
        }

        return new UploadProbe(null, false);
    }

    private static boolean supportBlockMissing(ItemFrame frame, BlockPos supportPos) {
        if (!(frame.level() instanceof ClientLevel clientLevel)) return false;

        ClientChunkCache chunkSource = clientLevel.getChunkSource();
        LevelChunk chunk = chunkSource.getChunk(
                SectionPos.blockToSectionCoord(supportPos.getX()),
                SectionPos.blockToSectionCoord(supportPos.getZ()),
                ChunkStatus.FULL,
                false
        );
        return chunk != null && chunk.getBlockState(supportPos).isAir();
    }

    private static void activateTerrainMapSurface(
            int entityId,
            ItemFrameExt ext,
            BlockPos supportPos,
            int mapLight
    ) {
        MapId mapId = ext.cachedFramedMapId();
        if (mapId == null) {
            setImmediateFallback(entityId, ext, supportPos);
            return;
        }

        // Don't scan map decorations here.
        // Labels are now rendered lazily only for the hovered frame.
        ItemFrameMapSurfaceRegistry.activate(entityId, mapId, mapLight);

        activateTerrainMesh(
                entityId,
                ext,
                supportPos,
                ItemFrameContentRenderMode.SECTION_MAP_SURFACE
        );
    }

    private static boolean activateReadyMapSurfaceWhileWaiting(
            ItemFrame frame,
            ItemFrameExt ext,
            BlockPos supportPos
    ) {
        MapId mapId = cachedMapId(frame, ext);
        if (mapId == null || MapPageCache.peekAtlasRefFast(mapId) == null) return false;

        // Don't scan map decorations here.
        // Labels are now rendered lazily only for the hovered frame.
        ItemFrameMapSurfaceRegistry.activate(
                frame.getId(),
                mapId,
                estimateMapLight(frame)
        );
        activateTerrainMesh(frame.getId(), ext, supportPos, ItemFrameContentRenderMode.SECTION_MAP_SURFACE);
        return true;
    }

    private static int estimateMapLight(ItemFrame frame) {
        BlockPos lightPos = BlockPos.containing(frame.getLightProbePosition(1.0F));
        int blockLight = frame.level().getBrightness(LightLayer.BLOCK, lightPos);
        boolean glowFrame = frame.is(EntityType.GLOW_ITEM_FRAME);
        if (glowFrame) {
            blockLight = Math.max(
                    blockLight,
                    net.minecraft.client.renderer.entity.ItemFrameRenderer.GLOW_FRAME_BRIGHTNESS
            );
        }

        int skyLight = frame.level().getBrightness(LightLayer.SKY, lightPos);
        int frameLight = LightCoordsUtil.pack(blockLight, skyLight);
        return ItemFrameRenderHelper.getMapLight(glowFrame, frameLight);
    }

    private static void deactivateUploadedMapSurface(int entityId) {
        ItemFrameMapSurfaceRegistry.deactivate(entityId);
    }

    private static void queueRebuildTargets(Set<BlockPos> targets, Runnable callback) {
        for (BlockPos target : targets) {
            if (callback != null) {
                SectionUpdateDispatcher.queueRebuildAtBlockPos(target, callback);
            } else {
                SectionUpdateDispatcher.queueRebuildAtBlockPos(target);
            }
        }
    }

    private static Set<BlockPos> targets(BlockPos oldSupport, BlockPos newSupport) {
        LinkedHashSet<BlockPos> targets = new LinkedHashSet<>(2);
        if (oldSupport != null && !BlockPos.ZERO.equals(oldSupport)) targets.add(oldSupport.immutable());
        if (newSupport != null && !BlockPos.ZERO.equals(newSupport)) targets.add(newSupport.immutable());
        return targets;
    }

    private static void clearUploadedSectionState(BlockPos oldSupport, BlockPos currentSupport) {
        for (BlockPos target : targets(oldSupport, currentSupport)) {
            ItemFrameSectionBuildBridge.clear(target);
            ItemFrameSectionUploadRegistry.clear(target);
        }
    }

    private static void clearMissingPayloadRetryState(BlockPos oldSupport, BlockPos currentSupport) {
        clearMissingPayloadRetryState(targets(oldSupport, currentSupport));
    }

    private static void clearMissingPayloadRetryState(Set<BlockPos> targets) {
        for (BlockPos target : targets) {
            clearMissingPayloadRetryState(target);
        }
    }

    private static void clearMissingPayloadRetryState(BlockPos supportPos) {
        long key = SectionRebuildCallbacks.keyFromBlockPos(supportPos);
        PENDING_MISSING_PAYLOAD_REBUILD_SECTIONS.remove(key);
        MISSING_PAYLOAD_RETRY_ATTEMPTS_BY_SECTION.remove(key);
    }

    private static void clearMissingPayloadRetryState() {
        PENDING_MISSING_PAYLOAD_REBUILD_SECTIONS.clear();
        MISSING_PAYLOAD_RETRY_ATTEMPTS_BY_SECTION.clear();
    }

    public static BlockPos supportPos(ItemFrame frame) {
        BlockPos framePos = frame.getPos();
        return framePos.relative(frame.getDirection().getOpposite()).immutable();
    }

    private static void ensureCachedItemState(ItemFrame frame, ItemFrameExt ext) {
        if (ext.cachedItemStateValid()) return;
        refreshCachedItemState(frame, ext);
    }

    private static void refreshCachedItemState(ItemFrame frame, ItemFrameExt ext) {
        ItemStack stack = frame.getItem();
        MapId mapId = frame.getFramedMapId(stack);
        ext.cachedFramedMapId(mapId);
        ext.cachedItemRawId(stack.isEmpty() ? 0 : BuiltInRegistries.ITEM.getId(stack.getItem()));
        ext.cachedComponentsHash(stack.isEmpty() || mapId != null ? 0 : tryComponentsHash(stack));
        ext.cachedItemStateValid(true);
    }

    public static boolean wasUploadedStateRefreshedThisTick(ItemFrameExt ext) {
        long gameTime = currentLevelGameTime;
        return gameTime != Long.MIN_VALUE && ext.uploadedStateRefreshGameTime() == gameTime;
    }

    private static boolean uploadedStateRefreshCurrent(ItemFrame frame, ItemFrameExt ext, long gameTime) {
        if (gameTime == Long.MIN_VALUE || ext.uploadedStateRefreshGameTime() != gameTime) return false;

        BlockPos refreshedSupport = ext.uploadedStateRefreshSupportPos();
        if (refreshedSupport == null) return false;

        BlockPos framePos = frame.getPos();
        Direction supportDirection = frame.getDirection().getOpposite();
        return refreshedSupport.getX() == framePos.getX() + supportDirection.getStepX()
                && refreshedSupport.getY() == framePos.getY() + supportDirection.getStepY()
                && refreshedSupport.getZ() == framePos.getZ() + supportDirection.getStepZ();
    }

    private static void markUploadedStateRefreshed(ItemFrameExt ext, BlockPos currentSupport, long gameTime) {
        ext.uploadedStateRefreshGameTime(gameTime);
        ext.uploadedStateRefreshSupportPos(currentSupport);
    }

    private static void invalidateUploadedStateRefresh(ItemFrameExt ext) {
        ext.uploadedStateRefreshGameTime(Long.MIN_VALUE);
        ext.uploadedStateRefreshSupportPos(null);
    }

    private static long currentGameTime(ItemFrame frame) {
        long gameTime = currentLevelGameTime;
        return gameTime != Long.MIN_VALUE ? gameTime : frame.level().getGameTime();
    }

    private static @Nullable MapId cachedMapId(ItemFrame frame, ItemFrameExt ext) {
        ensureCachedItemState(frame, ext);
        return ext.cachedFramedMapId();
    }

    private static long cachedRenderSignature(ItemFrame frame, ItemFrameExt ext) {
        ensureCachedItemState(frame, ext);
        return ItemFrameEligibility.computeRenderSignature(
                frame,
                ext.cachedFramedMapId(),
                ext.cachedItemRawId(),
                ext.cachedComponentsHash()
        );
    }

    private static int tryComponentsHash(ItemStack stack) {
        try { return stack.getComponents().hashCode(); }
        catch (Throwable ignored)
        { return 0; }
    }

    public static boolean isSectionSupportPos(BlockPos sectionOrigin, BlockPos supportPos) {
        return SectionPos.blockToSectionCoord(sectionOrigin.getX()) == SectionPos.blockToSectionCoord(supportPos.getX())
                && SectionPos.blockToSectionCoord(sectionOrigin.getY()) == SectionPos.blockToSectionCoord(supportPos.getY())
                && SectionPos.blockToSectionCoord(sectionOrigin.getZ()) == SectionPos.blockToSectionCoord(supportPos.getZ());
    }

    private static ItemFrameExt ext(ItemFrame frame) {
        return (ItemFrameExt) frame;
    }

    private static void clearLatchedTerrainSnapshot(ItemFrameExt ext) {
        ext.latchedTerrainSnapshot(false);
        ext.latchedContentRenderMode(ItemFrameContentRenderMode.NONE);
    }

    private static void activateLatchedTerrainVisual(
            int entityId,
            ItemFrameExt ext,
            BlockPos supportPos
    ) {
        ItemFrameContentRenderMode latchedMode = ext.latchedContentRenderMode();

        ext.terrainMeshReady(true);
        ext.terrainMeshActive(true);
        ext.renderImmediateWhileWaiting(false);
        ext.lastSupportPos(supportPos);
        ext.contentRenderMode(latchedMode);

        if (latchedMode != ItemFrameContentRenderMode.SECTION_MAP_SURFACE) {
            deactivateUploadedMapSurface(entityId);
        }
    }

    private static void ensureRuntimeRegistration(ItemFrame frame, ItemFrameExt ext) {
        ensureCachedItemState(frame, ext);

        if (frame.isRemoved()) {
            ItemFrameRemovalTracker.markRemoved(frame.getId());
            ItemFrameMapIndex.remove(frame.getId());
            ItemFrameSectionRegistry.remove(frame.getId());
            return;
        }

        ItemFrameRemovalTracker.markAdded(frame.getId());

        MapId mapId = ext.cachedFramedMapId();

        if (!ItemFrameEligibility.isFrameMeshSupported(frame, mapId)) {
            ItemFrameMapIndex.remove(frame.getId());
            ItemFrameSectionRegistry.remove(frame.getId());
            return;
        }

        ItemFrameSectionRegistry.upsert(frame, true);

        if (mapId != null) {
            ItemFrameMapIndex.upsert(frame, mapId, true);
        }
    }

    private static @Nullable ItemFrame findLiveItemFrame(ClientLevel level, int entityId) {
        Entity entity = level.getEntity(entityId);
        if (entity instanceof ItemFrame frame && !frame.isRemoved()) {
            ItemFrameRemovalTracker.markAdded(entityId);
            return frame;
        }

        for (Entity renderEntity : level.entitiesForRendering()) {
            if (!(renderEntity instanceof ItemFrame frame)) continue;
            if (frame.getId() != entityId) continue;
            if (frame.isRemoved()) continue;

            ItemFrameRemovalTracker.markAdded(entityId);
            return frame;
        }

        return null;
    }

    public static boolean shouldRunLiveFrameRecoverySeed() {
        return !sectionRegistryPrimed || liveSectionDiscoveryTicksRemaining > 0;
    }

    public static boolean shouldCollectLiveFramesDuringSectionCapture() {
        return !sectionRegistryPrimed || liveSectionDiscoveryTicksRemaining > 0;
    }

    public static boolean hasNamedDecorationLabels(Level level, @Nullable MapId mapId) {
        if (mapId == null) return false;

        int numericMapId = mapId.id();
        byte cached = cachedNamedDecorationLabels(numericMapId);

        if (cached == MAP_LABELS_PRESENT) return true;
        if (cached == MAP_LABELS_ABSENT) return false;

        MapItemSavedData mapData = level.getMapData(mapId);
        if (mapData == null) {
            cacheNamedDecorationLabels(numericMapId, MAP_LABELS_ABSENT);
            return false;
        }

        boolean hasLabels = false;
        for (MapDecoration decoration : mapData.getDecorations()) {
            if (decoration.renderOnFrame() && decoration.name().isPresent()) {
                hasLabels = true;
                break;
            }
        }

        cacheNamedDecorationLabels(numericMapId, hasLabels ? MAP_LABELS_PRESENT : MAP_LABELS_ABSENT);

        return hasLabels;
    }

    private static void activateReadyMapSurfaceForMissingSupport(
            ItemFrame frame,
            ItemFrameExt ext,
            BlockPos supportPos
    ) {
        MapId mapId = cachedMapId(frame, ext);
        if (mapId == null || MapPageCache.peekAtlasRefFast(mapId) == null) {
            setImmediateFallback(frame.getId(), ext, supportPos);
            return;
        }

        ItemFrameMapSurfaceRegistry.activate(
                frame.getId(),
                mapId,
                estimateMapLight(frame)
        );

        ext.terrainMeshReady(true);
        ext.terrainMeshActive(false);
        ext.renderImmediateWhileWaiting(true);
        ext.lastSupportPos(supportPos);
        ext.contentRenderMode(ItemFrameContentRenderMode.SECTION_MAP_SURFACE);
    }

    private static void clearNamedDecorationLabelCache(MapId mapId) {
        cacheNamedDecorationLabels(mapId.id(), (byte) 0);
    }

    private static void clearNamedDecorationLabelCache() {
        namedDecorationLabelsCache = new byte[16][];
    }

    private static byte cachedNamedDecorationLabels(int mapId) {
        if (mapId < 0) return 0;

        int pageIndex = mapId >>> MAP_LABEL_CACHE_PAGE_BITS;
        if (pageIndex >= namedDecorationLabelsCache.length) return 0;

        byte[] page = namedDecorationLabelsCache[pageIndex];
        return page != null ? page[mapId & MAP_LABEL_CACHE_PAGE_MASK] : 0;
    }

    private static void cacheNamedDecorationLabels(int mapId, byte value) {
        if (mapId < 0) return;

        int pageIndex = mapId >>> MAP_LABEL_CACHE_PAGE_BITS;
        ensureNamedDecorationLabelCachePageCapacity(pageIndex);

        byte[] page = namedDecorationLabelsCache[pageIndex];
        if (page == null) {
            if (value == 0) return;

            page = new byte[MAP_LABEL_CACHE_PAGE_SIZE];
            namedDecorationLabelsCache[pageIndex] = page;
        }

        page[mapId & MAP_LABEL_CACHE_PAGE_MASK] = value;
    }

    private static void ensureNamedDecorationLabelCachePageCapacity(int pageIndex) {
        if (pageIndex < namedDecorationLabelsCache.length) return;

        int newLength = namedDecorationLabelsCache.length;
        while (pageIndex >= newLength) {
            newLength *= 2;
        }

        byte[][] expanded = new byte[newLength][];
        System.arraycopy(namedDecorationLabelsCache, 0, expanded, 0, namedDecorationLabelsCache.length);
        namedDecorationLabelsCache = expanded;
    }

    private static void upsertMapIndexFromCachedState(ItemFrame frame, ItemFrameExt ext) {
        ensureCachedItemState(frame, ext);

        MapId mapId = ext.cachedFramedMapId();
        boolean meshEligible = mapId != null && ItemFrameEligibility.isFrameMeshSupported(frame, mapId);

        ItemFrameMapIndex.upsert(frame, mapId, meshEligible);
    }

    private static void upsertSectionRegistryFromCachedState(ItemFrame frame, ItemFrameExt ext) {
        ensureCachedItemState(frame, ext);
        ItemFrameSectionRegistry.upsert(
                frame,
                ItemFrameEligibility.isFrameMeshSupported(frame, ext.cachedFramedMapId())
        );
    }
}
