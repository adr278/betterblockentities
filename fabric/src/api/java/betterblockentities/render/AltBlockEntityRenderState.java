package betterblockentities.render;

/* minecraft */
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class AltBlockEntityRenderState {
    private BlockEntityType<?> blockEntityType;
    private BlockPos blockPos;

    public static void extractBase(final BlockEntity blockEntity, final AltBlockEntityRenderState state) {
        state.blockEntityType = blockEntity.getType();
        state.blockPos = blockEntity.getBlockPos();
    }

    public BlockEntityType<?> blockEntityType() {
        return this.blockEntityType;
    }

    public BlockPos blockPos() {
        return this.blockPos;
    }
}
