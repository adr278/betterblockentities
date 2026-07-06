package betterblockentities.client.render.immediate.util;

/* minecraft */
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

public final class VanillaBlockSupport {

    public static boolean isVanillaBlock(final BlockState state) {
        return "minecraft".equals(BuiltInRegistries.BLOCK.getKey(state.getBlock()).getNamespace());
    }

    public static boolean isVanillaChestBlock(final BlockState state) {
        return isAny(state, Blocks.CHEST, Blocks.TRAPPED_CHEST);
    }

    public static boolean isVanillaEnderChestBlock(final BlockState state) {
        return state.is(Blocks.ENDER_CHEST);
    }

    public static boolean isVanillaBedBlock(final BlockState state) {
        return isAny(state,
                Blocks.WHITE_BED,
                Blocks.ORANGE_BED,
                Blocks.MAGENTA_BED,
                Blocks.LIGHT_BLUE_BED,
                Blocks.YELLOW_BED,
                Blocks.LIME_BED,
                Blocks.PINK_BED,
                Blocks.GRAY_BED,
                Blocks.LIGHT_GRAY_BED,
                Blocks.CYAN_BED,
                Blocks.PURPLE_BED,
                Blocks.BLUE_BED,
                Blocks.BROWN_BED,
                Blocks.GREEN_BED,
                Blocks.RED_BED,
                Blocks.BLACK_BED
        );
    }

    public static boolean isVanillaShulkerBoxBlock(final BlockState state) {
        return isAny(state,
                Blocks.SHULKER_BOX,
                Blocks.WHITE_SHULKER_BOX,
                Blocks.ORANGE_SHULKER_BOX,
                Blocks.MAGENTA_SHULKER_BOX,
                Blocks.LIGHT_BLUE_SHULKER_BOX,
                Blocks.YELLOW_SHULKER_BOX,
                Blocks.LIME_SHULKER_BOX,
                Blocks.PINK_SHULKER_BOX,
                Blocks.GRAY_SHULKER_BOX,
                Blocks.LIGHT_GRAY_SHULKER_BOX,
                Blocks.CYAN_SHULKER_BOX,
                Blocks.PURPLE_SHULKER_BOX,
                Blocks.BLUE_SHULKER_BOX,
                Blocks.BROWN_SHULKER_BOX,
                Blocks.GREEN_SHULKER_BOX,
                Blocks.RED_SHULKER_BOX,
                Blocks.BLACK_SHULKER_BOX
        );
    }

    public static boolean isVanillaBannerBlock(final BlockState state) {
        return isAny(state,
                Blocks.WHITE_BANNER,
                Blocks.ORANGE_BANNER,
                Blocks.MAGENTA_BANNER,
                Blocks.LIGHT_BLUE_BANNER,
                Blocks.YELLOW_BANNER,
                Blocks.LIME_BANNER,
                Blocks.PINK_BANNER,
                Blocks.GRAY_BANNER,
                Blocks.LIGHT_GRAY_BANNER,
                Blocks.CYAN_BANNER,
                Blocks.PURPLE_BANNER,
                Blocks.BLUE_BANNER,
                Blocks.BROWN_BANNER,
                Blocks.GREEN_BANNER,
                Blocks.RED_BANNER,
                Blocks.BLACK_BANNER,
                Blocks.WHITE_WALL_BANNER,
                Blocks.ORANGE_WALL_BANNER,
                Blocks.MAGENTA_WALL_BANNER,
                Blocks.LIGHT_BLUE_WALL_BANNER,
                Blocks.YELLOW_WALL_BANNER,
                Blocks.LIME_WALL_BANNER,
                Blocks.PINK_WALL_BANNER,
                Blocks.GRAY_WALL_BANNER,
                Blocks.LIGHT_GRAY_WALL_BANNER,
                Blocks.CYAN_WALL_BANNER,
                Blocks.PURPLE_WALL_BANNER,
                Blocks.BLUE_WALL_BANNER,
                Blocks.BROWN_WALL_BANNER,
                Blocks.GREEN_WALL_BANNER,
                Blocks.RED_WALL_BANNER,
                Blocks.BLACK_WALL_BANNER
        );
    }

    public static boolean isVanillaSignBlock(final BlockState state) {
        return isAny(state,
                Blocks.OAK_SIGN,
                Blocks.SPRUCE_SIGN,
                Blocks.BIRCH_SIGN,
                Blocks.JUNGLE_SIGN,
                Blocks.ACACIA_SIGN,
                Blocks.CHERRY_SIGN,
                Blocks.DARK_OAK_SIGN,
                Blocks.MANGROVE_SIGN,
                Blocks.BAMBOO_SIGN,
                Blocks.CRIMSON_SIGN,
                Blocks.WARPED_SIGN,
                Blocks.OAK_WALL_SIGN,
                Blocks.SPRUCE_WALL_SIGN,
                Blocks.BIRCH_WALL_SIGN,
                Blocks.JUNGLE_WALL_SIGN,
                Blocks.ACACIA_WALL_SIGN,
                Blocks.CHERRY_WALL_SIGN,
                Blocks.DARK_OAK_WALL_SIGN,
                Blocks.MANGROVE_WALL_SIGN,
                Blocks.BAMBOO_WALL_SIGN,
                Blocks.CRIMSON_WALL_SIGN,
                Blocks.WARPED_WALL_SIGN
        );
    }

    public static boolean isVanillaHangingSignBlock(final BlockState state) {
        return isAny(state,
                Blocks.OAK_HANGING_SIGN,
                Blocks.SPRUCE_HANGING_SIGN,
                Blocks.BIRCH_HANGING_SIGN,
                Blocks.JUNGLE_HANGING_SIGN,
                Blocks.ACACIA_HANGING_SIGN,
                Blocks.CHERRY_HANGING_SIGN,
                Blocks.DARK_OAK_HANGING_SIGN,
                Blocks.MANGROVE_HANGING_SIGN,
                Blocks.BAMBOO_HANGING_SIGN,
                Blocks.CRIMSON_HANGING_SIGN,
                Blocks.WARPED_HANGING_SIGN,
                Blocks.OAK_WALL_HANGING_SIGN,
                Blocks.SPRUCE_WALL_HANGING_SIGN,
                Blocks.BIRCH_WALL_HANGING_SIGN,
                Blocks.JUNGLE_WALL_HANGING_SIGN,
                Blocks.ACACIA_WALL_HANGING_SIGN,
                Blocks.CHERRY_WALL_HANGING_SIGN,
                Blocks.DARK_OAK_WALL_HANGING_SIGN,
                Blocks.MANGROVE_WALL_HANGING_SIGN,
                Blocks.BAMBOO_WALL_HANGING_SIGN,
                Blocks.CRIMSON_WALL_HANGING_SIGN,
                Blocks.WARPED_WALL_HANGING_SIGN
        );
    }

    public static boolean isVanillaDecoratedPotBlock(final BlockState state) {
        return state.is(Blocks.DECORATED_POT);
    }

    public static boolean isVanillaBlockEntity(final BlockEntity blockEntity, final BlockEntityType<?> expectedType) {
        return blockEntity.getType() == expectedType && isVanillaBlock(blockEntity.getBlockState());
    }

    private static boolean isAny(final BlockState state, final Block... blocks) {
        for (Block block : blocks) {
            if (state.is(block)) return true;
        }

        return false;
    }
}
