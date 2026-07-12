package betterblockentities.data;

/* minecraft */
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.BlockEntityTypes;

public enum SupportedBlockEntityTypes {
    CHEST(BlockEntityTypes.CHEST),
    ENDER_CHEST(BlockEntityTypes.ENDER_CHEST),
    TRAPPED_CHEST(BlockEntityTypes.TRAPPED_CHEST),
    SIGN(BlockEntityTypes.SIGN),
    HANGING_SIGN(BlockEntityTypes.HANGING_SIGN),
    SHULKER_BOX(BlockEntityTypes.SHULKER_BOX),
    DECORATED_POT(BlockEntityTypes.DECORATED_POT),
    BANNER(BlockEntityTypes.BANNER),
    BELL(BlockEntityTypes.BELL),
    COPPER_GOLEM_STATUE(BlockEntityTypes.COPPER_GOLEM_STATUE);

    private final BlockEntityType<?> blockEntityType;

    SupportedBlockEntityTypes(BlockEntityType<?> blockEntityType) {
        this.blockEntityType = blockEntityType;
    }

    public BlockEntityType<?> type() {
        return this.blockEntityType;
    }
}
