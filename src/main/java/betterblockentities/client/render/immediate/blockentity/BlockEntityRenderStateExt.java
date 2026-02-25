package betterblockentities.client.render.immediate.blockentity;

/* minecraft */
import net.minecraft.world.level.block.entity.BlockEntity;

public interface BlockEntityRenderStateExt {
    void blockEntity(BlockEntity blockEntity);
    BlockEntity blockEntity();
}
