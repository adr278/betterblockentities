package betterblockentities.client.chunk.pipeline.itemframe;

/* local */
import betterblockentities.client.render.immediate.entity.ItemFrameRuntimeHelper;

/* minecraft */
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.world.entity.decoration.ItemFrame;

/* java */
import java.util.HashMap;
import java.util.LinkedHashMap;

/* fastutil */
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

public final class ItemFrameSectionRegistry {
    private record MountKey(BlockPos supportPos, Direction facing) {}
    private record TrackedFrame(long sectionKey, MountKey mountKey, ItemFrame frame) {}

    private static final Object LOCK = new Object();
    private static final ItemFrame[] EMPTY = new ItemFrame[0];
    private static final HashMap<Integer, TrackedFrame> FRAMES_BY_ENTITY_ID = new HashMap<>();
    private static final HashMap<MountKey, Integer> ENTITY_ID_BY_MOUNT = new HashMap<>();
    private static final Long2ObjectOpenHashMap<LinkedHashMap<Integer, ItemFrame>> FRAMES_BY_SECTION =
            new Long2ObjectOpenHashMap<>();

    private ItemFrameSectionRegistry() {}

    public static void upsert(ItemFrame frame) {
        upsert(frame, ItemFrameEligibility.isFrameMeshSupported(frame));
    }

    public static void upsert(ItemFrame frame, boolean meshEligible) {
        if (frame.isRemoved() || !ItemFrameEligibility.optimizationEnabled() || !meshEligible) {
            remove(frame.getId());
            return;
        }

        BlockPos supportPos = ItemFrameRuntimeHelper.supportPos(frame);
        long sectionKey = SectionPos.of(supportPos).asLong();
        MountKey mountKey = new MountKey(supportPos, frame.getDirection());

        synchronized (LOCK) {
            TrackedFrame oldTrackedFrame = FRAMES_BY_ENTITY_ID.get(frame.getId());
            Integer mountedEntityId = ENTITY_ID_BY_MOUNT.get(mountKey);

            if (mountedEntityId != null && mountedEntityId != frame.getId()) {
                TrackedFrame mountedFrame = FRAMES_BY_ENTITY_ID.get(mountedEntityId);
                if (mountedFrame != null) {
                    removeTrackedFrameLocked(mountedEntityId, mountedFrame);
                }
            }

            if (oldTrackedFrame != null
                    && oldTrackedFrame.sectionKey() == sectionKey
                    && oldTrackedFrame.mountKey().equals(mountKey)) {
                ENTITY_ID_BY_MOUNT.put(mountKey, frame.getId());
                return;
            }

            if (oldTrackedFrame != null) {
                removeTrackedFrameLocked(frame.getId(), oldTrackedFrame);
            }

            LinkedHashMap<Integer, ItemFrame> frames = FRAMES_BY_SECTION.get(sectionKey);
            if (frames == null) {
                frames = new LinkedHashMap<>();
                FRAMES_BY_SECTION.put(sectionKey, frames);
            }

            frames.put(frame.getId(), frame);
            FRAMES_BY_ENTITY_ID.put(frame.getId(), new TrackedFrame(sectionKey, mountKey, frame));
            ENTITY_ID_BY_MOUNT.put(mountKey, frame.getId());
        }
    }

    public static void remove(int entityId) {
        synchronized (LOCK) {
            TrackedFrame trackedFrame = FRAMES_BY_ENTITY_ID.get(entityId);
            if (trackedFrame == null) return;

            removeTrackedFrameLocked(entityId, trackedFrame);
        }
    }

    public static ItemFrame[] framesForSection(SectionPos sectionPos) {
        synchronized (LOCK) {
            LinkedHashMap<Integer, ItemFrame> frames = FRAMES_BY_SECTION.get(sectionPos.asLong());
            if (frames == null || frames.isEmpty()) return EMPTY;

            return frames.values().toArray(ItemFrame[]::new);
        }
    }

    public static void clear() {
        synchronized (LOCK) {
            FRAMES_BY_ENTITY_ID.clear();
            ENTITY_ID_BY_MOUNT.clear();
            FRAMES_BY_SECTION.clear();
        }
    }

    private static void removeTrackedFrameLocked(int entityId, TrackedFrame trackedFrame) {
        FRAMES_BY_ENTITY_ID.remove(entityId);
        ENTITY_ID_BY_MOUNT.remove(trackedFrame.mountKey());

        LinkedHashMap<Integer, ItemFrame> frames = FRAMES_BY_SECTION.get(trackedFrame.sectionKey());
        if (frames == null) return;

        frames.remove(entityId);
        if (frames.isEmpty()) FRAMES_BY_SECTION.remove(trackedFrame.sectionKey());
    }

    public static boolean hasFramesForSection(SectionPos sectionPos) {
        synchronized (LOCK) {
            LinkedHashMap<Integer, ItemFrame> frames = FRAMES_BY_SECTION.get(sectionPos.asLong());
            return frames != null && !frames.isEmpty();
        }
    }
}
