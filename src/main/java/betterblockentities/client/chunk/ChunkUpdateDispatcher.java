package betterblockentities.client.chunk;

/* local */
import betterblockentities.client.BetterBlockEntities;

/* minecraft */
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

public class ChunkUpdateDispatcher {
    public static void queueRebuildAtBlockPos(Level world, long pos) {
        try {
            BlockPos posObj = BlockPos.of(pos);
            var state = world.getBlockState(posObj);
            Minecraft.getInstance().levelRenderer.blockChanged(world, posObj, state, state, 8);
        } catch (Exception e) {
            BetterBlockEntities.getLogger().error("Error: Failed to update render section at {}", pos, e);
        }
    }
}
