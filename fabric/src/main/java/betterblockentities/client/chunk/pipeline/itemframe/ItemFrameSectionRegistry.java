package betterblockentities.client.chunk.pipeline.itemframe;

/* local */
import betterblockentities.client.render.immediate.entity.ItemFrameRuntimeHelper;

/* minecraft */
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ItemFrame;

/* java */
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;

public final class ItemFrameSectionRegistry {
    private record MountKey(BlockPos supportPos, Direction facing) {}
    private record TrackedFrame(BlockPos supportPos, MountKey mountKey, ItemFrame frame, long sectionKey) {}

    private static final Object LOCK = new Object();
    private static final ItemFrame[] EMPTY = new ItemFrame[0];
    private static final BlockPos[] EMPTY_SUPPORTS = new BlockPos[0];
    private static final HashMap<Integer, TrackedFrame> FRAMES_BY_ENTITY_ID = new HashMap<>();
    private static final HashMap<MountKey, Integer> ENTITY_ID_BY_MOUNT = new HashMap<>();
    private static final HashMap<BlockPos, LinkedHashMap<Integer, ItemFrame>> FRAMES_BY_SUPPORT = new HashMap<>();
    private static final HashMap<Long, LinkedHashSet<BlockPos>> SUPPORTS_BY_SECTION = new HashMap<>();

    public static void upsert(ItemFrame frame, boolean meshEligible) {
        if (frame.isRemoved() || !ItemFrameEligibility.optimizationEnabled() || !meshEligible) {
            remove(frame.getId());
            return;
        }

        BlockPos supportPos = ItemFrameRuntimeHelper.supportPos(frame).immutable();
        MountKey mountKey = new MountKey(supportPos, frame.getDirection());
        long sectionKey = SectionPos.asLong(
                SectionPos.blockToSectionCoord(supportPos.getX()),
                SectionPos.blockToSectionCoord(supportPos.getY()),
                SectionPos.blockToSectionCoord(supportPos.getZ())
        );

        synchronized (LOCK) {
            TrackedFrame oldTrackedFrame = FRAMES_BY_ENTITY_ID.get(frame.getId());
            Integer mountedEntityId = ENTITY_ID_BY_MOUNT.get(mountKey);

            if (mountedEntityId != null && mountedEntityId != frame.getId()) {
                TrackedFrame mountedFrame = FRAMES_BY_ENTITY_ID.get(mountedEntityId);
                if (mountedFrame != null) {
                    if (trackedItemFrameStillOwned(mountedFrame, mountKey)) {
                        if (mountedEntityId < frame.getId()) {
                            // Keep the older owner stable so placement attempts
                            // cannot replace the existing item frame's visual.
                            if (oldTrackedFrame != null) removeTrackedFrameLocked(frame.getId(), oldTrackedFrame);
                            return;
                        }
                    }

                    removeTrackedFrameLocked(mountedEntityId, mountedFrame);
                } else {
                    ENTITY_ID_BY_MOUNT.remove(mountKey);
                }
            }

            if (oldTrackedFrame != null
                    && oldTrackedFrame.supportPos().equals(supportPos)
                    && oldTrackedFrame.mountKey().equals(mountKey)) {
                ENTITY_ID_BY_MOUNT.put(mountKey, frame.getId());
                return;
            }

            if (oldTrackedFrame != null) removeTrackedFrameLocked(frame.getId(), oldTrackedFrame);

            LinkedHashMap<Integer, ItemFrame> frames = FRAMES_BY_SUPPORT.computeIfAbsent(supportPos, _ -> new LinkedHashMap<>());

            frames.put(frame.getId(), frame);
            FRAMES_BY_ENTITY_ID.put(frame.getId(), new TrackedFrame(supportPos, mountKey, frame, sectionKey));
            ENTITY_ID_BY_MOUNT.put(mountKey, frame.getId());

            SUPPORTS_BY_SECTION
                    .computeIfAbsent(sectionKey, _ -> new LinkedHashSet<>())
                    .add(supportPos);
        }
    }

    public static void remove(int entityId) {
        synchronized (LOCK) {
            TrackedFrame trackedFrame = FRAMES_BY_ENTITY_ID.get(entityId);
            if (trackedFrame == null) return;

            removeTrackedFrameLocked(entityId, trackedFrame);
        }
    }

    public static ItemFrame[] framesForSupport(BlockPos supportPos) {
        synchronized (LOCK) {
            LinkedHashMap<Integer, ItemFrame> frames = FRAMES_BY_SUPPORT.get(supportPos);
            if (frames == null || frames.isEmpty()) return EMPTY;

            return frames.values().toArray(ItemFrame[]::new);
        }
    }

    public static BlockPos[] supportsForSection(int sectionX, int sectionY, int sectionZ) {
        long sectionKey = SectionPos.asLong(sectionX, sectionY, sectionZ);
        synchronized (LOCK) {
            LinkedHashSet<BlockPos> supports = SUPPORTS_BY_SECTION.get(sectionKey);
            if (supports == null || supports.isEmpty()) return EMPTY_SUPPORTS;
            return supports.toArray(BlockPos[]::new);
        }
    }

    public static void clear() {
        synchronized (LOCK) {
            FRAMES_BY_ENTITY_ID.clear();
            ENTITY_ID_BY_MOUNT.clear();
            FRAMES_BY_SUPPORT.clear();
            SUPPORTS_BY_SECTION.clear();
        }
    }

    private static void removeTrackedFrameLocked(int entityId, TrackedFrame trackedFrame) {
        FRAMES_BY_ENTITY_ID.remove(entityId);
        ENTITY_ID_BY_MOUNT.remove(trackedFrame.mountKey(), entityId);

        LinkedHashMap<Integer, ItemFrame> frames = FRAMES_BY_SUPPORT.get(trackedFrame.supportPos());
        if (frames == null) return;

        frames.remove(entityId);
        if (!frames.isEmpty()) return;

        FRAMES_BY_SUPPORT.remove(trackedFrame.supportPos());
        LinkedHashSet<BlockPos> supports = SUPPORTS_BY_SECTION.get(trackedFrame.sectionKey());
        if (supports == null) return;

        supports.remove(trackedFrame.supportPos());
        if (supports.isEmpty()) SUPPORTS_BY_SECTION.remove(trackedFrame.sectionKey());
    }

    private static boolean trackedItemFrameStillOwned(TrackedFrame trackedFrame, MountKey mountKey) {
        ItemFrame trackedEntity = trackedFrame.frame();
        if (trackedEntity == null || trackedEntity.isRemoved()) return false;
        if (!trackedFrame.mountKey().equals(mountKey)) return false;

        if (trackedEntity.level() instanceof ClientLevel clientLevel) {
            Entity live = clientLevel.getEntity(trackedEntity.getId());
            return live == trackedEntity;
        }

        return true;
    }
}
