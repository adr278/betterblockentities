package betterblockentities.client.chunk.pipeline.itemframe;

/* local */
import betterblockentities.client.chunk.section.SectionRebuildCallbacks;

/* minecraft */
import net.minecraft.core.BlockPos;

/* java */
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/* annotations */
import org.jspecify.annotations.Nullable;

public final class ItemFrameSectionUploadRegistry {
    public record UploadedFrame(
            long renderSignature,
            ItemFrameContentRenderMode contentRenderMode,
            int mapLight
    ) {}

    public record UploadedSection(
            int sectionX,
            int sectionY,
            int sectionZ,
            ItemFrameSectionAppender appender,
            Map<Integer, UploadedFrame> frames
    ) {}

    private ItemFrameSectionUploadRegistry() {}

    private static final ConcurrentHashMap<Long, UploadedSection> UPLOADED_SECTIONS = new ConcurrentHashMap<>();

    public static void update(
            int sectionX,
            int sectionY,
            int sectionZ,
            @Nullable ItemFrameSectionAppender appender
    ) {
        long key = SectionRebuildCallbacks.keyFromSectionPos(sectionX, sectionY, sectionZ);

        if (appender == null || appender.entries().isEmpty()) {
            UPLOADED_SECTIONS.remove(key);
            return;
        }

        HashMap<Integer, UploadedFrame> frames = new HashMap<>(appender.entries().size());
        for (ItemFrameSectionAppender.Entry entry : appender.entries()) {
            if (ItemFrameRemovalTracker.isRemoved(entry.entityId())) continue;

            frames.put(entry.entityId(), new UploadedFrame(
                    entry.renderSignature(),
                    entry.contentRenderMode(),
                    entry.mapLight()
            ));
        }

        if (frames.isEmpty()) {
            UPLOADED_SECTIONS.remove(key);
            return;
        }

        UPLOADED_SECTIONS.put(key, new UploadedSection(
                sectionX,
                sectionY,
                sectionZ,
                appender,
                Map.copyOf(frames)
        ));
    }

    public static void clear(int sectionX, int sectionY, int sectionZ) {
        long key = SectionRebuildCallbacks.keyFromSectionPos(sectionX, sectionY, sectionZ);
        UPLOADED_SECTIONS.remove(key);
    }

    public static void clear(BlockPos supportPos) {
        long key = SectionRebuildCallbacks.keyFromBlockPos(supportPos);
        UPLOADED_SECTIONS.remove(key);
    }

    public static @Nullable UploadedFrame getFrame(BlockPos supportPos, int entityId) {
        long key = SectionRebuildCallbacks.keyFromBlockPos(supportPos);
        UploadedSection section = UPLOADED_SECTIONS.get(key);
        return section != null ? section.frames().get(entityId) : null;
    }

    public static void clearAll() { UPLOADED_SECTIONS.clear(); }
}
