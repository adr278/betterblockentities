package betterblockentities.client.chunk.pipeline.itemframe;

/* local */
import betterblockentities.client.chunk.pipeline.shelf.CacheKeys;
import betterblockentities.client.chunk.section.SectionRebuildCallbacks;
import betterblockentities.client.gui.config.ConfigCache;

/* minecraft */
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.ItemStack;

public final class ItemFrameEligibility {
    public record Evaluation(
            ItemFrameContentRenderMode contentRenderMode,
            ItemFrameItemModelBuilder.CaptureResult itemCapture
    ) {}

    public static boolean optimizationEnabled() { return ConfigCache.masterOptimize && ConfigCache.optimizeItemFrames; }

    public static long computeRenderSignature(
            ItemFrame frame,
            int itemRawId,
            int componentsHash
    ) {
        long signature = 0x4F8A2D1C9B3E6751L;
        signature = CacheKeys.mix64(signature, frame.getDirection().ordinal());
        signature = CacheKeys.mix64(signature, frame.getRotation());
        signature = CacheKeys.mix64(signature, frame.isInvisible() ? 1L : 0L);
        signature = CacheKeys.mix64(signature, frame.is(EntityType.GLOW_ITEM_FRAME) ? 1L : 0L);

        if (frame.getItem().isEmpty()) return CacheKeys.mix64(signature, 0L);

        return CacheKeys.mix64(signature, CacheKeys.packSig0(itemRawId, componentsHash));
    }

    public static boolean isFrameMeshSupported(ItemFrame frame) {
        if (!optimizationEnabled()) return false;
        if (isMapFrame(frame, frame.getItem())) return false;
        if (frame.isInvisible()) return true;

        boolean glowFrame = frame.is(EntityType.GLOW_ITEM_FRAME);
        return ItemFrameModelCapture.getFrameMesh(glowFrame) != null;
    }

    public static Evaluation evaluateForTerrainEmission(ItemFrame frame) {
        ItemStack stack = frame.getItem();
        if (stack.isEmpty()) {
            return new Evaluation(ItemFrameContentRenderMode.NONE, ItemFrameItemModelBuilder.CaptureResult.EMPTY);
        }

        if (isMapFrame(frame, stack)) {
            return new Evaluation(ItemFrameContentRenderMode.IMMEDIATE_ITEM, ItemFrameItemModelBuilder.CaptureResult.EMPTY);
        }

        ItemFrameItemModelBuilder.CaptureResult itemCapture =
                ItemFrameItemModelBuilder.captureMesh(frame, stack);
        return new Evaluation(itemCapture.contentRenderMode(), itemCapture);
    }

    public static void invalidateAllCachesOnReload() { SectionRebuildCallbacks.clearAll(); }

    private static boolean isMapFrame(ItemFrame frame, ItemStack stack) { return frame.getFramedMapId(stack) != null; }
}
