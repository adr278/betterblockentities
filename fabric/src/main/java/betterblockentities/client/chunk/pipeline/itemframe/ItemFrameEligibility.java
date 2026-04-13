package betterblockentities.client.chunk.pipeline.itemframe;

/* local */
import betterblockentities.client.chunk.pipeline.shelf.CacheKeys;
import betterblockentities.client.chunk.section.SectionRebuildCallbacks;
import betterblockentities.client.gui.config.ConfigCache;

/* minecraft */
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.maps.MapId;

/* annotations */
import org.jspecify.annotations.Nullable;

public final class ItemFrameEligibility {
    public record Evaluation(
            ItemFrameContentRenderMode contentRenderMode,
            ItemFrameItemModelBuilder.CaptureResult itemCapture,
            MapId mapId
    ) {}

    private ItemFrameEligibility() {}

    public static boolean optimizationEnabled() {
        return ConfigCache.masterOptimize && ConfigCache.optimizeItemFrames;
    }

    public static long computeRenderSignature(ItemFrame frame, Evaluation evaluation) {
        ItemStack stack = frame.getItem();
        if (stack.isEmpty()) return computeRenderSignature(frame, null, 0, 0);

        MapId mapId = evaluation.mapId();
        if (mapId != null) return computeRenderSignature(frame, mapId, 0, 0);

        int itemRawId = BuiltInRegistries.ITEM.getId(stack.getItem());
        int componentsHash = tryComponentsHash(stack);
        return computeRenderSignature(frame, null, itemRawId, componentsHash);
    }

    public static long computeRenderSignature(
            ItemFrame frame,
            @Nullable MapId mapId,
            int itemRawId,
            int componentsHash
    ) {
        long signature = 0x4F8A2D1C9B3E6751L;
        signature = CacheKeys.mix64(signature, frame.getDirection().ordinal());
        signature = CacheKeys.mix64(signature, frame.getRotation());
        signature = CacheKeys.mix64(signature, frame.isInvisible() ? 1L : 0L);
        signature = CacheKeys.mix64(signature, frame.is(EntityType.GLOW_ITEM_FRAME) ? 1L : 0L);

        if (mapId != null) return CacheKeys.mix64(signature, mapId.hashCode());
        if (frame.getItem().isEmpty()) return CacheKeys.mix64(signature, 0L);

        return CacheKeys.mix64(signature, CacheKeys.packSig0(itemRawId, componentsHash));
    }

    public static boolean isFrameMeshSupported(ItemFrame frame) {
        if (!optimizationEnabled()) return false;
        if (frame.isInvisible()) return true;

        return isFrameMeshSupported(frame, getMapId(frame, frame.getItem()));
    }

    public static boolean isFrameMeshSupported(ItemFrame frame, @Nullable MapId mapId) {
        if (!optimizationEnabled()) return false;
        if (frame.isInvisible()) return true;

        boolean glowFrame = frame.is(EntityType.GLOW_ITEM_FRAME);
        return ItemFrameModelCapture.getFrameMesh(glowFrame, mapId != null) != null;
    }

    public static Evaluation evaluateForSectionCapture(ItemFrame frame) {
        ItemStack stack = frame.getItem();
        if (stack.isEmpty()) {
            return new Evaluation(ItemFrameContentRenderMode.NONE, ItemFrameItemModelBuilder.CaptureResult.EMPTY, null);
        }

        MapId mapId = getMapId(frame, stack);
        if (mapId != null) {
            MapLifecycleState lifecycleState = MapPageCache.lifecycleForCapture(mapId);

            return switch (lifecycleState) {
                case MESHED_READY -> new Evaluation(
                        ItemFrameContentRenderMode.SECTION_MAP_SURFACE,
                        ItemFrameItemModelBuilder.CaptureResult.EMPTY,
                        mapId
                );
                case NORMAL_FALLBACK -> new Evaluation(
                        ItemFrameContentRenderMode.IMMEDIATE_ITEM,
                        ItemFrameItemModelBuilder.CaptureResult.EMPTY,
                        mapId
                );
                case PENDING_DATA, NONE -> new Evaluation(
                        ItemFrameContentRenderMode.NONE,
                        ItemFrameItemModelBuilder.CaptureResult.EMPTY,
                        mapId
                );
            };
        }

        ItemFrameItemModelBuilder.CaptureResult itemCapture =
                ItemFrameItemModelBuilder.getOrCaptureMesh(frame, stack);
        return new Evaluation(itemCapture.contentRenderMode(), itemCapture, null);
    }

    public static void invalidateAllCachesOnReload() {
        MapPageCache.invalidateAllCachesOnReload();
        ItemFrameSectionBuildBridge.clearAll();
        ItemFrameSectionUploadRegistry.clearAll();
        ItemFrameMapIndex.clear();
        SectionRebuildCallbacks.clearAll();
    }

    public static void invalidateRuntimeStateOnLevelChange() {
        MapPageCache.invalidateRuntimeStateOnLevelChange();
        ItemFrameSectionBuildBridge.clearAll();
        ItemFrameSectionUploadRegistry.clearAll();
        ItemFrameMapIndex.clear();
        SectionRebuildCallbacks.clearAll();
    }

    private static MapId getMapId(ItemFrame frame, ItemStack stack) { return frame.getFramedMapId(stack); }

    private static int tryComponentsHash(ItemStack stack) {
        try { return stack.getComponents().hashCode(); }
        catch (Throwable ignored)
        { return 0; }
    }
}
