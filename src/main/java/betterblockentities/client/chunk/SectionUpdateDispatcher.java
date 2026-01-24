package betterblockentities.client.chunk;

/* local */
import betterblockentities.client.BBE;

/* minecraft */
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/**
 * Used to update the render section (16^3 blocks) in which this block lies, this can fail if called async
 */
public class SectionUpdateDispatcher {
    public static void queueRebuildAtBlockPos(Level world, long pos) {
        try {
            BlockPos posObj = BlockPos.of(pos);
            var state = world.getBlockState(posObj);
            Minecraft.getInstance().levelRenderer.blockChanged(world, posObj, state, state, 8);
        } catch (Exception e) {
            BBE.getLogger().error("Failed to update render section at {}", pos, e);
        }
    }

    public static void queueUpdateAllSections() {
        try {
            Minecraft.getInstance().levelRenderer.allChanged();
        } catch (Exception e) {
            BBE.getLogger().error("Reloading all render sections failed!", e);
        }
    }
}
