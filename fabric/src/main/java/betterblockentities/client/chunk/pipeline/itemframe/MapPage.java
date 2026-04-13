package betterblockentities.client.chunk.pipeline.itemframe;

/* minecraft */
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

/* annotations */
import org.jspecify.annotations.Nullable;

public final class MapPage {
    private final MapAtlasRef ref;
    private volatile @Nullable MapId mapId;
    private volatile @Nullable MapItemSavedData mapData;
    private volatile long contentHash = Long.MIN_VALUE;
    private volatile boolean uploadQueued;

    public MapPage(MapAtlasRef ref) { this.ref = ref; }

    public MapAtlasRef ref() { return this.ref; }

    public @Nullable MapId mapId() { return this.mapId; }

    public @Nullable MapItemSavedData mapData() { return this.mapData; }

    public void assign(@Nullable MapId mapId) {
        this.mapId = mapId;
        this.mapData = null;
        this.contentHash = Long.MIN_VALUE;
        this.uploadQueued = false;
    }

    public void unassign() {
        this.mapId = null;
        this.mapData = null;
        this.contentHash = Long.MIN_VALUE;
        this.uploadQueued = false;
    }

    public long contentHash() { return this.contentHash; }

    public void markUploaded(MapItemSavedData mapData, long contentHash) {
        this.mapData = mapData;
        this.contentHash = contentHash;
    }

    public void markStale() {
        this.mapData = null;
        this.contentHash = Long.MIN_VALUE;
        this.uploadQueued = false;
    }

    public boolean isReady() { return this.contentHash != Long.MIN_VALUE; }

    public boolean uploadQueued() { return this.uploadQueued; }

    public void uploadQueued(boolean uploadQueued) { this.uploadQueued = uploadQueued; }
}
