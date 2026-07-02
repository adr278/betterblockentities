package betterblockentities.client.tasks;

/* local */
import betterblockentities.client.model.geometry.GeometryRegistry;
import betterblockentities.client.model.geometry.ModelGenerator;
import betterblockentities.platform.GlobalScope;

/**
 * Tasks to be executed after resource reload, these tasks should be scheduled with
 * {@link betterblockentities.client.tasks.TaskScheduler -> scheduleOnReload }
 */
public class ResourceTasks {
    public static int FAILED = 0xFFFF;
    public static int COMPLETE = 0x0000;

    public static int populateGeometryRegistry() {
        try {
            if (!GeometryRegistry.getCache().isEmpty()) {
                GlobalScope.LOGGER.info("Clearing geometry registry!");
                GeometryRegistry.clearCache();
            }
            if (ModelGenerator.generateAppend() == COMPLETE) {
                GlobalScope.LOGGER.info("Geometry registry populated! Task successfully completed");
                return COMPLETE;
            }
            else {
                GlobalScope.LOGGER.error("Could not prepare the necessary geometry because the entityModelSet was null. Check previous logs!");
                return FAILED;
            }
        } catch (Throwable t) {
            GlobalScope.LOGGER.error("Setup/bake task failed! because of internal error", t);
            return FAILED;
        }
    }
}
