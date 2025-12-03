package betterblockentities.chunk;

/* minecraft */
import betterblockentities.BetterBlockEntities;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/*
    TODO: add more rebuild options, like rebuild then perform task...
*/

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
