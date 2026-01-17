package betterblockentities.client.render.immediate.blockentity;

/* java/misc */
import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;

/**
 * Global data structure for keeping track of what BlockEntities are animating and if they have
 * additional render passes which we have to render them for with its BlockEntityRenderer
 */
public class BlockEntityTracker {
    public static final LongSet animMap = new LongOpenHashSet();
    public static final Long2IntMap extraRenderPasses = new Long2IntOpenHashMap();
}
