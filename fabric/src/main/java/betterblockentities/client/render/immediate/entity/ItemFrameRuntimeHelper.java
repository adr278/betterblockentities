package betterblockentities.client.render.immediate.entity;

/* local */
import betterblockentities.client.chunk.pipeline.itemframe.ItemFrameContentRenderMode;
import betterblockentities.client.chunk.pipeline.itemframe.ItemFrameEligibility;
import betterblockentities.client.chunk.pipeline.itemframe.ItemFrameSectionRegistry;
import betterblockentities.client.chunk.section.SectionUpdateDispatcher;
import betterblockentities.client.render.immediate.entity.extensions.ItemFrameExt;
import betterblockentities.client.tasks.TaskScheduler;

/* minecraft */
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.ItemStack;

/* java */
import java.util.LinkedHashSet;

/* annotations */
import org.jspecify.annotations.Nullable;

public final class ItemFrameRuntimeHelper {
    private static int levelEpoch;
    private static final ThreadLocal<Integer> packetEntityRemovalDepth = ThreadLocal.withInitial(() -> 0);
    private static final ThreadLocal<Integer> packetEntityDataUpdateDepth = ThreadLocal.withInitial(() -> 0);

    private ItemFrameRuntimeHelper() {}

    public static void onAdded(ItemFrame frame) {
        if (isLiveClientFrame(frame)) return;

        ItemFrameExt ext = ext(frame);
        BlockPos supportPos = supportPos(frame);
        refreshCachedItemState(frame, ext);
        invalidateTerrainStateRefresh(ext);
        ext.lastSupportPos(supportPos);

        if (!optimizationEnabled() || !ItemFrameEligibility.isFrameMeshSupported(frame)) {
            ItemFrameSectionRegistry.remove(frame.getId());
            setImmediateFallback(ext, supportPos);
            return;
        }

        ItemFrameSectionRegistry.upsert(frame, true);
        scheduleRebuild(frame, supportPos, supportPos, false);

        int scheduledLevelEpoch = levelEpoch;
        TaskScheduler.schedule(() -> {
            if (scheduledLevelEpoch != levelEpoch || frame.isRemoved()) return;
            onFrameContentsChanged(frame);
        });
    }

    public static void onRemoved(ItemFrame frame) {
        ItemFrameExt ext = ext(frame);
        invalidateTerrainStateRefresh(ext);

        BlockPos oldSupport = ext.lastSupportPos();
        BlockPos currentSupport = supportPos(frame);

        ItemFrameSectionRegistry.remove(frame.getId());
        ext.terrainMeshReady(true);
        ext.terrainMeshActive(false);
        ext.contentRenderMode(ItemFrameContentRenderMode.NONE);

        queueRebuildTargets(oldSupport, currentSupport, null);
    }

    public static void onFrameContentsChanged(ItemFrame frame) {
        if (isLiveClientFrame(frame)) return;
        if (frame.isRemoved()) {
            onRemoved(frame);
            return;
        }

        ItemFrameExt ext = ext(frame);
        ensureCachedItemState(frame, ext);

        refreshCachedItemState(frame, ext);
        invalidateTerrainStateRefresh(ext);

        BlockPos oldSupport = ext.lastSupportPos();
        BlockPos newSupport = supportPos(frame);
        if (ext.terrainMeshActive()
                && !oldSupport.equals(newSupport)
                && !handlingPacketEntityDataUpdate()) {
            // Keep the already-submitted terrain mesh attached to the previous
            // support section until the entity is actually removed.
            return;
        }

        upsertSectionRegistryFromCachedState(frame);
        updateForStateChange(frame, oldSupport, newSupport);
    }

    public static void onSupportPossiblyChanged(ItemFrame frame) {
        if (isLiveClientFrame(frame)) return;
        if (frame.isRemoved()) {
            onRemoved(frame);
            return;
        }

        ItemFrameExt ext = ext(frame);
        invalidateTerrainStateRefresh(ext);
        ensureCachedItemState(frame, ext);

        BlockPos oldSupport = ext.lastSupportPos();
        BlockPos newSupport = supportPos(frame);
        if (oldSupport.equals(newSupport)) return;
        if (ext.terrainMeshActive() && !handlingPacketEntityDataUpdate()) return;

        upsertSectionRegistryFromCachedState(frame);
        updateForStateChange(frame, oldSupport, newSupport);
    }

    public static void onResourceReloaded() {
        levelEpoch++;
        ItemFrameSectionRegistry.clear();

        if (!optimizationEnabled()) return;

        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return;

        LinkedHashSet<Integer> seenFrameIds = new LinkedHashSet<>();
        for (Entity entity : level.entitiesForRendering()) {
            if (!(entity instanceof ItemFrame frame) || frame.isRemoved()) continue;
            if (!seenFrameIds.add(frame.getId())) continue;

            onFrameContentsChanged(frame);
        }
    }

    public static void beginPacketEntityRemoval() { packetEntityRemovalDepth.set(packetEntityRemovalDepth.get() + 1); }

    public static void endPacketEntityRemoval() {
        int depth = packetEntityRemovalDepth.get() - 1;
        if (depth <= 0) {
            packetEntityRemovalDepth.remove();
            return;
        }

        packetEntityRemovalDepth.set(depth);
    }

    public static void beginPacketEntityDataUpdate() {
        packetEntityDataUpdateDepth.set(packetEntityDataUpdateDepth.get() + 1);
    }

    public static void endPacketEntityDataUpdate() {
        int depth = packetEntityDataUpdateDepth.get() - 1;
        if (depth <= 0) {
            packetEntityDataUpdateDepth.remove();
            return;
        }

        packetEntityDataUpdateDepth.set(depth);
    }

    public static boolean handlingPacketEntityDataUpdate() {
        return packetEntityDataUpdateDepth.get() > 0;
    }

    public static void refreshTerrainState(ItemFrame frame) {
        if (!optimizationEnabled()) return;
        if (isLiveClientFrame(frame)) return;

        ItemFrameExt ext = ext(frame);
        long gameTime = frame.level().getGameTime();
        if (terrainStateRefreshCurrent(frame, ext, gameTime)) return;

        ensureCachedItemState(frame, ext);
        BlockPos previousSupport = ext.lastSupportPos();
        BlockPos currentSupport = supportPos(frame);
        markTerrainStateRefreshed(ext, currentSupport, gameTime);

        if (!ItemFrameEligibility.isFrameMeshSupported(frame)) {
            ItemFrameSectionRegistry.remove(frame.getId());
            setImmediateFallback(ext, currentSupport);
            queueRebuildTargets(previousSupport, currentSupport, null);
            return;
        }

        if (!currentSupport.equals(previousSupport)) {
            if (ext.terrainMeshActive()) return;

            updateForStateChange(frame, previousSupport, currentSupport);
            return;
        }

        if (!ext.terrainMeshReady()) return;
        if (!ext.terrainMeshActive()) updateForStateChange(frame, previousSupport, currentSupport);
    }

    private static void updateForStateChange(
            ItemFrame frame,
            BlockPos oldSupport,
            BlockPos newSupport
    ) {
        ItemFrameExt ext = ext(frame);

        boolean newEligible = optimizationEnabled() && ItemFrameEligibility.isFrameMeshSupported(frame);
        boolean preserveUntilRebuild = ext.terrainMeshActive() && newEligible;

        if (!newEligible) {
            ItemFrameSectionRegistry.remove(frame.getId());
            setImmediateFallback(ext, newSupport);
            queueRebuildTargets(oldSupport, newSupport, null);
            return;
        }

        ItemFrameSectionRegistry.upsert(frame, true);
        scheduleRebuild(frame, oldSupport, newSupport, preserveUntilRebuild);
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
        } else {
            ext.terrainMeshReady(true);
            ext.terrainMeshActive(true);
        }

        invalidateTerrainStateRefresh(ext);
        ext.lastSupportPos(newSupport);

        int scheduledLevelEpoch = levelEpoch;
        BlockPos expectedSupport = newSupport.immutable();
        long expectedSignature = cachedRenderSignature(frame, ext);

        Runnable callback = () -> {
            if (scheduledLevelEpoch != levelEpoch || frame.isRemoved()) return;
            activateAfterRebuild(frame, expectedSupport, expectedSignature);
        };

        queueRebuildTargets(oldSupport, newSupport, callback);
    }

    private static boolean optimizationEnabled() { return ItemFrameEligibility.optimizationEnabled(); }

    private static void setImmediateFallback(ItemFrameExt ext, BlockPos supportPos) {
        ext.terrainMeshReady(true);
        ext.terrainMeshActive(false);
        ext.lastSupportPos(supportPos);
        ext.contentRenderMode(ItemFrameContentRenderMode.IMMEDIATE_ITEM);
    }

    private static void activateTerrainMesh(
            ItemFrameExt ext,
            BlockPos supportPos,
            ItemFrameContentRenderMode contentRenderMode
    ) {
        ext.terrainMeshReady(true);
        ext.terrainMeshActive(true);
        ext.lastSupportPos(supportPos);
        ext.contentRenderMode(contentRenderMode);
    }

    private static void queueRebuildTargets(
            BlockPos oldSupport,
            BlockPos newSupport,
            @Nullable Runnable newSupportCallback
    ) {
        boolean queuedNewSupport = false;

        if (validSupportTarget(oldSupport) && !oldSupport.equals(newSupport)) {
            SectionUpdateDispatcher.queueRebuildAtBlockPos(oldSupport);
        }

        if (validSupportTarget(newSupport)) {
            queuedNewSupport = true;
            if (newSupportCallback != null) {
                SectionUpdateDispatcher.queueRebuildAtBlockPos(newSupport, newSupportCallback);
            } else {
                SectionUpdateDispatcher.queueRebuildAtBlockPos(newSupport);
            }
        }

        if (!queuedNewSupport && newSupportCallback != null) newSupportCallback.run();
    }

    private static boolean validSupportTarget(@Nullable BlockPos supportPos) {
        return supportPos != null && !BlockPos.ZERO.equals(supportPos);
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
        ext.cachedItemRawId(stack.isEmpty() ? 0 : BuiltInRegistries.ITEM.getId(stack.getItem()));
        ext.cachedComponentsHash(stack.isEmpty() ? 0 : tryComponentsHash(stack));
        ext.cachedItemStateValid(true);
    }

    private static boolean terrainStateRefreshCurrent(ItemFrame frame, ItemFrameExt ext, long gameTime) {
        if (gameTime == Long.MIN_VALUE || ext.terrainStateRefreshGameTime() != gameTime) return false;

        BlockPos refreshedSupport = ext.terrainStateRefreshSupportPos();
        if (refreshedSupport == null) return false;

        BlockPos framePos = frame.getPos();
        Direction supportDirection = frame.getDirection().getOpposite();
        return refreshedSupport.getX() == framePos.getX() + supportDirection.getStepX()
                && refreshedSupport.getY() == framePos.getY() + supportDirection.getStepY()
                && refreshedSupport.getZ() == framePos.getZ() + supportDirection.getStepZ();
    }

    private static void markTerrainStateRefreshed(ItemFrameExt ext, BlockPos currentSupport, long gameTime) {
        ext.terrainStateRefreshGameTime(gameTime);
        ext.terrainStateRefreshSupportPos(currentSupport);
    }

    private static void invalidateTerrainStateRefresh(ItemFrameExt ext) {
        ext.terrainStateRefreshGameTime(Long.MIN_VALUE);
        ext.terrainStateRefreshSupportPos(null);
    }

    private static long cachedRenderSignature(ItemFrame frame, ItemFrameExt ext) {
        ensureCachedItemState(frame, ext);
        return ItemFrameEligibility.computeRenderSignature(
                frame,
                ext.cachedItemRawId(),
                ext.cachedComponentsHash()
        );
    }

    private static int tryComponentsHash(ItemStack stack) {
        try { return stack.getComponents().hashCode(); }
        catch (Throwable ignored)
        { return 0; }
    }

    private static ItemFrameExt ext(ItemFrame frame) {
        return (ItemFrameExt) frame;
    }

    private static void activateAfterRebuild(
            ItemFrame frame,
            BlockPos expectedSupport,
            long expectedSignature
    ) {
        if (isLiveClientFrame(frame)) return;

        ItemFrameExt ext = ext(frame);
        ensureCachedItemState(frame, ext);

        BlockPos currentSupport = supportPos(frame);
        if (!ItemFrameEligibility.isFrameMeshSupported(frame)) {
            setImmediateFallback(ext, currentSupport);
            return;
        }

        if (!currentSupport.equals(expectedSupport)) {
            onSupportPossiblyChanged(frame);
            return;
        }

        if (cachedRenderSignature(frame, ext) != expectedSignature) {
            setImmediateFallback(ext, currentSupport);
            onFrameContentsChanged(frame);
            return;
        }

        ItemFrameEligibility.Evaluation evaluation =
                ItemFrameEligibility.evaluateForTerrainEmission(frame);
        activateTerrainMesh(ext, currentSupport, evaluation.contentRenderMode());
    }

    private static void upsertSectionRegistryFromCachedState(ItemFrame frame) {
        ItemFrameSectionRegistry.upsert(frame, ItemFrameEligibility.isFrameMeshSupported(frame));
    }

    private static boolean isLiveClientFrame(ItemFrame frame) {
        if (!(frame.level() instanceof ClientLevel clientLevel)) return true;

        Entity live = clientLevel.getEntity(frame.getId());
        return live != frame || frame.isRemoved();
    }
}
