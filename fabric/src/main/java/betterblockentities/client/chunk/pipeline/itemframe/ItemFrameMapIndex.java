package betterblockentities.client.chunk.pipeline.itemframe;

/* local */
import betterblockentities.client.render.immediate.entity.ItemFrameRuntimeHelper;

/* minecraft */
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.phys.Vec3;

/* java */
import java.util.HashMap;

/* fastutil */
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

/* annotations */
import org.jspecify.annotations.Nullable;

public final class ItemFrameMapIndex {
    public record Entry(
            int entityId,
            BlockPos supportPos,
            SectionPos sectionPos,
            MapId mapId,
            Vec3 entityPos,
            Direction facing,
            int rotation,
            boolean invisible,
            boolean meshEligible
    ) {}

    public record SectionBucket(
            SectionPos sectionPos,
            Entry[] entries
    ) {}

    private record MountKey(BlockPos supportPos, Direction facing) {}

    private static final Object LOCK = new Object();

    private static final Int2ObjectOpenHashMap<Entry> ENTRIES_BY_ENTITY_ID = new Int2ObjectOpenHashMap<>();
    private static final Int2ObjectOpenHashMap<Int2ObjectOpenHashMap<Entry>> ENTRIES_BY_MAP_ID = new Int2ObjectOpenHashMap<>();
    private static final Long2ObjectOpenHashMap<SectionEntries> ENTRIES_BY_SECTION = new Long2ObjectOpenHashMap<>();
    private static final HashMap<MountKey, Entry> ENTRIES_BY_MOUNT = new HashMap<>();

    private static volatile Entry[] ENTRY_SNAPSHOT = new Entry[0];
    private static volatile SectionBucket[] SECTION_BUCKET_SNAPSHOT = new SectionBucket[0];
    private static volatile MapId[] ACTIVE_MAP_ID_SNAPSHOT = new MapId[0];
    private static volatile int VERSION;

    private static boolean entrySnapshotDirty;
    private static boolean sectionBucketSnapshotDirty;
    private static boolean activeMapIdSnapshotDirty;

    private ItemFrameMapIndex() {}

    public static void upsert(
            ItemFrame frame,
            @Nullable MapId mapId,
            boolean meshEligible
    ) {
        if (frame.isRemoved() || mapId == null || !meshEligible) {
            remove(frame.getId());
            return;
        }

        BlockPos supportPos = ItemFrameRuntimeHelper.supportPos(frame);
        Entry entry = new Entry(
                frame.getId(),
                supportPos,
                SectionPos.of(supportPos),
                mapId,
                frame.position(),
                frame.getDirection(),
                frame.getRotation(),
                frame.isInvisible(),
                true
        );

        synchronized (LOCK) {
            Entry oldEntry = ENTRIES_BY_ENTITY_ID.get(frame.getId());
            MountKey mountKey = mountKey(entry);
            Entry mountedEntry = ENTRIES_BY_MOUNT.get(mountKey);
            boolean removedMountedEntry = false;

            if (mountedEntry != null && mountedEntry.entityId() != frame.getId()) {
                removeEntryLocked(mountedEntry);
                removedMountedEntry = true;
            }

            if (entry.equals(oldEntry)) {
                if (removedMountedEntry) {
                    ENTRIES_BY_MOUNT.put(mountKey, entry);
                    markAllSnapshotsDirty();
                }
                return;
            }

            if (oldEntry != null) removeEntryLocked(oldEntry);

            ENTRIES_BY_ENTITY_ID.put(frame.getId(), entry);
            addToMapIndex(entry);
            addToSectionIndex(entry);
            ENTRIES_BY_MOUNT.put(mountKey, entry);
            markAllSnapshotsDirty();
        }
    }

    public static void remove(int entityId) {
        synchronized (LOCK) {
            Entry oldEntry = ENTRIES_BY_ENTITY_ID.get(entityId);
            if (oldEntry == null) return;

            removeEntryLocked(oldEntry);
            markAllSnapshotsDirty();
        }
    }

    public static MapId[] activeMapIdSnapshot() {
        MapId[] snapshot = ACTIVE_MAP_ID_SNAPSHOT;
        if (!activeMapIdSnapshotDirty) return snapshot;

        synchronized (LOCK) {
            if (!activeMapIdSnapshotDirty) return ACTIVE_MAP_ID_SNAPSHOT;

            int[] mapIds = ENTRIES_BY_MAP_ID.keySet().toIntArray();
            MapId[] rebuilt = new MapId[mapIds.length];

            for (int i = 0; i < mapIds.length; i++) {
                rebuilt[i] = new MapId(mapIds[i]);
            }

            ACTIVE_MAP_ID_SNAPSHOT = rebuilt;
            activeMapIdSnapshotDirty = false;
            return rebuilt;
        }
    }

    public static Entry[] entrySnapshot() {
        Entry[] snapshot = ENTRY_SNAPSHOT;
        if (!entrySnapshotDirty) return snapshot;

        synchronized (LOCK) {
            if (!entrySnapshotDirty) return ENTRY_SNAPSHOT;

            Entry[] rebuilt = ENTRIES_BY_ENTITY_ID.values().toArray(new Entry[0]);
            ENTRY_SNAPSHOT = rebuilt;
            entrySnapshotDirty = false;
            return rebuilt;
        }
    }

    public static SectionBucket[] sectionBucketSnapshot() {
        SectionBucket[] snapshot = SECTION_BUCKET_SNAPSHOT;
        if (!sectionBucketSnapshotDirty) return snapshot;

        synchronized (LOCK) {
            if (!sectionBucketSnapshotDirty) return SECTION_BUCKET_SNAPSHOT;

            SectionBucket[] rebuilt = new SectionBucket[ENTRIES_BY_SECTION.size()];
            int index = 0;

            for (SectionEntries sectionEntries : ENTRIES_BY_SECTION.values()) {
                Entry[] entries = sectionEntries.entries.values().toArray(new Entry[0]);
                rebuilt[index++] = new SectionBucket(sectionEntries.sectionPos, entries);
            }

            SECTION_BUCKET_SNAPSHOT = rebuilt;
            sectionBucketSnapshotDirty = false;
            return rebuilt;
        }
    }

    public static Entry[] entriesForMapSnapshot(MapId mapId) {
        synchronized (LOCK) {
            Int2ObjectOpenHashMap<Entry> entries = ENTRIES_BY_MAP_ID.get(mapId.id());
            if (entries == null || entries.isEmpty()) return new Entry[0];

            return entries.values().toArray(new Entry[0]);
        }
    }

    public static void clear() {
        synchronized (LOCK) {
            if (ENTRIES_BY_ENTITY_ID.isEmpty()
                    && ENTRIES_BY_MAP_ID.isEmpty()
                    && ENTRIES_BY_SECTION.isEmpty()) {
                return;
            }

            ENTRIES_BY_ENTITY_ID.clear();
            ENTRIES_BY_MAP_ID.clear();
            ENTRIES_BY_SECTION.clear();
            ENTRIES_BY_MOUNT.clear();

            ENTRY_SNAPSHOT = new Entry[0];
            SECTION_BUCKET_SNAPSHOT = new SectionBucket[0];
            ACTIVE_MAP_ID_SNAPSHOT = new MapId[0];

            entrySnapshotDirty = false;
            sectionBucketSnapshotDirty = false;
            activeMapIdSnapshotDirty = false;

            bumpVersion();
        }
    }

    private static void addToMapIndex(Entry entry) {
        Int2ObjectOpenHashMap<Entry> entries = ENTRIES_BY_MAP_ID.get(entry.mapId().id());
        if (entries == null) {
            entries = new Int2ObjectOpenHashMap<>();
            ENTRIES_BY_MAP_ID.put(entry.mapId().id(), entries);
        }

        entries.put(entry.entityId(), entry);
    }

    private static void addToSectionIndex(Entry entry) {
        long sectionKey = entry.sectionPos().asLong();
        SectionEntries sectionEntries = ENTRIES_BY_SECTION.get(sectionKey);
        if (sectionEntries == null) {
            sectionEntries = new SectionEntries(entry.sectionPos());
            ENTRIES_BY_SECTION.put(sectionKey, sectionEntries);
        }

        sectionEntries.entries.put(entry.entityId(), entry);
    }

    private static void removeEntryLocked(Entry entry) {
        ENTRIES_BY_ENTITY_ID.remove(entry.entityId());
        ENTRIES_BY_MOUNT.remove(mountKey(entry));
        removeFromSecondaryIndexes(entry);
    }

    private static void removeFromSecondaryIndexes(Entry entry) {
        Int2ObjectOpenHashMap<Entry> mapEntries = ENTRIES_BY_MAP_ID.get(entry.mapId().id());
        if (mapEntries != null) {
            mapEntries.remove(entry.entityId());
            if (mapEntries.isEmpty()) ENTRIES_BY_MAP_ID.remove(entry.mapId().id());
        }

        long sectionKey = entry.sectionPos().asLong();
        SectionEntries sectionEntries = ENTRIES_BY_SECTION.get(sectionKey);
        if (sectionEntries != null) {
            sectionEntries.entries.remove(entry.entityId());
            if (sectionEntries.entries.isEmpty()) ENTRIES_BY_SECTION.remove(sectionKey);
        }
    }

    private static MountKey mountKey(Entry entry) { return new MountKey(entry.supportPos(), entry.facing()); }

    private static void markAllSnapshotsDirty() {
        entrySnapshotDirty = true;
        sectionBucketSnapshotDirty = true;
        activeMapIdSnapshotDirty = true;
        bumpVersion();
    }

    private static void bumpVersion() {
        VERSION = VERSION == Integer.MAX_VALUE ? 1 : VERSION + 1;
    }

    private static final class SectionEntries {
        private final SectionPos sectionPos;
        private final Int2ObjectOpenHashMap<Entry> entries = new Int2ObjectOpenHashMap<>();

        private SectionEntries(SectionPos sectionPos) {
            this.sectionPos = sectionPos;
        }
    }
}
