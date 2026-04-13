package betterblockentities.client.chunk.pipeline.itemframe;

/* minecraft */
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;

/* java */
import java.util.concurrent.ConcurrentHashMap;

/* annotations */
import org.jspecify.annotations.Nullable;

public final class ItemFrameSectionBuildBridge {
    private ItemFrameSectionBuildBridge() {}

    private static final ThreadLocal<ItemFrameSectionAppender> CURRENT = new ThreadLocal<>();
    private static final ConcurrentHashMap<Long, ItemFrameSectionAppender> LATEST = new ConcurrentHashMap<>();

    public static void publish(BlockPos sectionOrigin, @Nullable ItemFrameSectionAppender appender) {
        long key = SectionPos.asLong(
                SectionPos.blockToSectionCoord(sectionOrigin.getX()),
                SectionPos.blockToSectionCoord(sectionOrigin.getY()),
                SectionPos.blockToSectionCoord(sectionOrigin.getZ())
        );

        if (appender == null || appender.entries().isEmpty()) {
            LATEST.remove(key);
            return;
        }

        LATEST.put(key, appender);
    }

    public static @Nullable ItemFrameSectionAppender consume(SectionPos sectionPos) {
        return LATEST.get(sectionPos.asLong());
    }

    public static @Nullable ItemFrameSectionAppender latest(SectionPos sectionPos) {
        return LATEST.get(sectionPos.asLong());
    }

    public static void clear(BlockPos supportPos) { LATEST.remove(SectionPos.of(supportPos).asLong()); }

    public static void begin(@Nullable ItemFrameSectionAppender appender) {
        if (appender == null || appender.entries().isEmpty()) {
            CURRENT.remove();
            return;
        }

        CURRENT.set(appender);
    }

    public static @Nullable ItemFrameSectionAppender current() {
        return CURRENT.get();
    }

    public static @Nullable ItemFrameSectionAppender take() {
        ItemFrameSectionAppender appender = CURRENT.get();
        CURRENT.remove();
        return appender;
    }

    public static void clearAll() {
        CURRENT.remove();
        LATEST.clear();
    }
}
