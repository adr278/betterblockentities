package betterblockentities.client.render.immediate.blockentity.extentions;

import betterblockentities.client.render.immediate.blockentity.manager.InstancedBlockEntityManager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public interface ChestBlockEntityExt {
    InstancedBlockEntityManager bbeManager();
    ChestBlockEntity bbeOtherHalf(Level level, BlockState state);
}
