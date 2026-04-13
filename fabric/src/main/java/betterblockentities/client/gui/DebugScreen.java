package betterblockentities.client.gui;

/* local */
import betterblockentities.client.BBE;
import betterblockentities.client.chunk.pipeline.itemframe.MapPageCache;
import betterblockentities.client.render.immediate.entity.extensions.ItemFrameExt;
import betterblockentities.client.render.immediate.entity.renderers.BBEItemFrameRenderer;
import betterblockentities.client.tasks.ManagerTasks;

/* minecraft */
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.debug.DebugEntryCategory;
import net.minecraft.client.gui.components.debug.DebugScreenDisplayer;
import net.minecraft.client.gui.components.debug.DebugScreenEntry;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;

/* java/misc */
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class DebugScreen implements DebugScreenEntry {
    private static final Identifier WORK_QUEUE_GROUP = Identifier.fromNamespaceAndPath("bbe", "work_queue");
    private static final Identifier MAPS_GROUP = Identifier.fromNamespaceAndPath("bbe", "maps");
    private static final long MAP_DEBUG_CACHE_INTERVAL_NANOS = 250_000_000L;

    private static @Nullable Level cachedMapDebugLevel;
    private static long cachedMapDebugNanos = Long.MIN_VALUE;
    private static @Nullable String cachedMapDebugLine;

    @Override
    public void display(DebugScreenDisplayer debugScreenDisplayer, @Nullable Level level, @Nullable LevelChunk levelChunk, @Nullable LevelChunk levelChunk2) {
        debugScreenDisplayer.addToGroup(WORK_QUEUE_GROUP, "[BBE] Manager Work Queue: " + ManagerTasks.WORK_QUEUE.size());
        debugScreenDisplayer.addToGroup(MAPS_GROUP, mapDebugLine());
    }

    private static String mapDebugLine() {
        int totalMaps = 0;
        int customRenderer = 0;
        int active = 0;
        int readyOnly = 0;
        int waiting = 0;

        Minecraft minecraft = Minecraft.getInstance();
        var clientLevel = minecraft.level;
        long now = System.nanoTime();
        if (cachedMapDebugLine != null
                && cachedMapDebugLevel == clientLevel
                && cachedMapDebugNanos != Long.MIN_VALUE
                && now - cachedMapDebugNanos < MAP_DEBUG_CACHE_INTERVAL_NANOS) {
            return cachedMapDebugLine;
        }

        if (clientLevel != null) {
            var dispatcher = minecraft.getEntityRenderDispatcher();
            for (var entity : clientLevel.entitiesForRendering()) {
                if (!(entity instanceof ItemFrame frame) || frame.isRemoved()) {
                    continue;
                }
                if (frame.getFramedMapId(frame.getItem()) == null) {
                    continue;
                }

                totalMaps++;
                if (dispatcher.getRenderer(frame) instanceof BBEItemFrameRenderer<?>) {
                    customRenderer++;
                }

                ItemFrameExt ext = (ItemFrameExt) frame;
                if (ext.terrainMeshActive()) {
                    active++;
                } else if (ext.terrainMeshReady()) {
                    readyOnly++;
                } else {
                    waiting++;
                }
            }
        }

        cachedMapDebugLevel = clientLevel;
        cachedMapDebugNanos = now;
        cachedMapDebugLine = "[BBE] Maps: indexed="
                        + MapPageCache.debugIndexedMapCount()
                        + " assigned="
                        + MapPageCache.debugAssignedMapCount()
                        + " ready="
                        + MapPageCache.debugReadyMapCount()
                        + " fallback="
                        + MapPageCache.debugFallbackMapCount()
                        + " total="
                        + totalMaps
                        + " custom="
                        + customRenderer
                        + " active="
                        + active
                        + " readyOnly="
                        + readyOnly
                        + " waiting="
                        + waiting;

        return cachedMapDebugLine;
    }

    @Override public boolean isAllowed(boolean bl) {
        return true;
    }
    @Override public @NonNull DebugEntryCategory category() {
        return BBE.GlobalScope.DEBUG_CATEGORY;
    }
}
