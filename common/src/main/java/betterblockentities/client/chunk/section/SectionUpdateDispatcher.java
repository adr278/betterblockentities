package betterblockentities.client.chunk.section;

/* local */
import betterblockentities.client.tasks.TaskScheduler;

/* minecraft */
import betterblockentities.platform.GlobalScope;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

/* sodium */
import net.caffeinemc.mods.sodium.client.render.SodiumWorldRenderer;

/**
 * Utility class for executing render section rebuild tasks
 */
public class SectionUpdateDispatcher {
    public static void queueRebuildAtBlockPos(BlockPos pos) {
        try {
            TaskScheduler.schedule(() -> {
                if (Minecraft.getInstance().level == null) return;

                SodiumWorldRenderer sodiumWorldRenderer = SodiumWorldRenderer.instanceNullable();
                if (sodiumWorldRenderer != null) {
                    sodiumWorldRenderer.scheduleRebuildForBlockArea(
                            pos.getX(), pos.getY(), pos.getZ(),
                            pos.getX(), pos.getY(), pos.getZ(),
                            false
                    );
                }
            });
        } catch (Exception e) {
            GlobalScope.LOGGER.error("Failed to rebuild terrain section!", e);
            SectionRebuildCallbacks.remove(pos);
        }
    }

    /**
     * rebuild section with a fence callback (runnable runs after section rebuild is complete)
     */
    public static void queueRebuildAtBlockPos(BlockPos pos, Runnable onUploadedFence) {
        SectionRebuildCallbacks.await(pos, onUploadedFence);
        queueRebuildAtBlockPos(pos);
    }

    public static void queueUpdateAllSections() {
        try {
            SodiumWorldRenderer sodiumWorldRenderer = SodiumWorldRenderer.instanceNullable();
            if (sodiumWorldRenderer != null) {
                sodiumWorldRenderer.scheduleTerrainUpdate();
            }
        } catch (Exception e) {
            GlobalScope.LOGGER.error("Reloading terrain sections failed!", e);
        }
    }
}
