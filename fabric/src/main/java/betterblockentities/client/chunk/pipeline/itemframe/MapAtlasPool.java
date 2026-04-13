package betterblockentities.client.chunk.pipeline.itemframe;

/* minecraft */
import net.minecraft.world.level.saveddata.maps.MapId;

/* java */
import java.util.ArrayList;
import java.util.List;

/* fastutil */
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;

/* annotations */
import org.jspecify.annotations.Nullable;

public final class MapAtlasPool {
    public record AssignmentDelta(
            List<MapId> assigned,
            List<MapId> released
    ) {}

    private final ArrayList<MapPage> pages = new ArrayList<>();
    private final ArrayList<MapId> assignedScratch = new ArrayList<>();
    private final ArrayList<MapId> releasedScratch = new ArrayList<>();
    private final AssignmentDelta assignmentDelta = new AssignmentDelta(this.assignedScratch, this.releasedScratch);
    private final Int2ObjectOpenHashMap<MapPage> pagesByMapId = new Int2ObjectOpenHashMap<>();
    private final IntOpenHashSet desiredScratch = new IntOpenHashSet();
    private int initializedSlotCount = -1;
    private int assignmentVersion = 1;

    public void invalidate() {
        this.pages.clear();
        this.pagesByMapId.clear();
        this.assignedScratch.clear();
        this.releasedScratch.clear();
        this.desiredScratch.clear();
        this.initializedSlotCount = -1;
        bumpAssignmentVersion();
    }

    public boolean ensureInitialized() {
        MapAtlasBudgetPlanner.BudgetResult budget = MapAtlasManager.budgetNullable();
        if (budget == null) return true;

        int slotCount = budget.safeBudget();
        if (this.initializedSlotCount == slotCount) return slotCount <= 0;

        this.pages.clear();
        this.pagesByMapId.clear();
        bumpAssignmentVersion();

        for (int slotId = 0; slotId < slotCount; slotId++) {
            MapAtlasRef ref = MapAtlasManager.refForSlot(slotId);
            if (ref == null) break;

            this.pages.add(new MapPage(ref));
        }

        this.initializedSlotCount = this.pages.size();
        return this.pages.isEmpty();
    }

    public @Nullable MapPage peek(MapId mapId) { return this.pagesByMapId.get(mapId.id()); }

    public boolean isAssigned(MapId mapId) { return this.pagesByMapId.containsKey(mapId.id()); }

    public int assignedMapCount() { return this.pagesByMapId.size(); }

    public int assignmentVersion() { return this.assignmentVersion; }

    public int readyMapCount() {
        int count = 0;
        for (MapPage page : this.pages) {
            if (page.mapId() != null && page.isReady()) {
                count++;
            }
        }

        return count;
    }

    public boolean needsAssignmentRefresh(IntSet activeMapIds, int activeMapCount) {
        int desiredAssignedCount = Math.min(activeMapCount, this.pages.size());
        if (activeMapCount > this.pages.size()) return true;
        if (this.pagesByMapId.size() < desiredAssignedCount) return true;

        for (MapPage page : this.pages) {
            MapId mapId = page.mapId();
            if (mapId != null && !activeMapIds.contains(mapId.id())) return true;
        }

        return false;
    }

    public AssignmentDelta syncAssignments(List<MapId> desiredMapIds, IntSet activeMapIds) {
        this.releasedScratch.clear();
        this.assignedScratch.clear();
        this.desiredScratch.clear();

        for (MapId mapId : desiredMapIds) this.desiredScratch.add(mapId.id());

        for (MapPage page : this.pages) {
            MapId pageMapId = page.mapId();
            if (pageMapId == null) continue;
            int numericMapId = pageMapId.id();
            if (activeMapIds.contains(numericMapId) && this.desiredScratch.contains(numericMapId)) continue;

            this.pagesByMapId.remove(numericMapId);
            page.unassign();
            this.releasedScratch.add(pageMapId);
        }

        for (MapId mapId : desiredMapIds) {
            if (this.pagesByMapId.containsKey(mapId.id())) continue;

            MapPage target = this.findUnassignedPage();
            if (target == null) break;

            target.assign(mapId);
            this.pagesByMapId.put(mapId.id(), target);
            this.assignedScratch.add(mapId);
        }

        if (!this.assignedScratch.isEmpty() || !this.releasedScratch.isEmpty()) bumpAssignmentVersion();

        return this.assignmentDelta;
    }

    private @Nullable MapPage findUnassignedPage() {
        for (MapPage page : this.pages) if (page.mapId() == null) return page;

        return null;
    }

    private void bumpAssignmentVersion() {
        if (this.assignmentVersion == Integer.MAX_VALUE) {
            this.assignmentVersion = 1;
        } else {
            this.assignmentVersion++;
        }
    }
}
