package betterblockentities.client.tasks;

/* local */
import betterblockentities.client.BBE;
import betterblockentities.client.chunk.SectionUpdateDispatcher;
import betterblockentities.client.model.geometry.GeometryRegistry;
import betterblockentities.client.model.geometry.ModelGenerator;

public class Tasks {
    public static int TASK_FAILED = 0xFFFF;
    public static int TASK_COMPLETE = 0x0000;

    public static int populateGeometryRegistry() {
        try {
            if (!GeometryRegistry.getCache().isEmpty()) {
                BBE.getLogger().info("Clearing geometry registry!");
                GeometryRegistry.clearCache();
            }
            if (ModelGenerator.generateAppend() == TASK_COMPLETE) {
                BBE.getLogger().info("Geometry registry populated! Task successfully completed");
                return TASK_COMPLETE;
            }
            else {
                BBE.getLogger().error("Could not prepare the necessary geometry because the entityModelSet was null. Check previous logs!");
                return TASK_FAILED;
            }
        } catch (Throwable t) {
            BBE.getLogger().error("Setup/bake task failed! because of internal error", t);
            return TASK_FAILED;
        }
    }

    public static void reloadRenderSections() {
        SectionUpdateDispatcher.queueUpdateAllSections();
    }
}
