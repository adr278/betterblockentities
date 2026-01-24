package betterblockentities.client.model;

/* minecraft */
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.state.ChestRenderState;
import net.minecraft.client.resources.model.Material;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.CopperChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.DecoratedPotPatterns;
import net.minecraft.world.level.block.entity.EnderChestBlockEntity;
import net.minecraft.world.level.block.entity.TrappedChestBlockEntity;

/* java/misc */
import java.util.Optional;

/**
 * rebuild these functions so we can safely use them asynchronously when meshing
 * to avoid invoking these from each renderer (not thread-safe)
 */
public class MaterialSelector {
    public static Material getDPSideMaterial(Optional<Item> optional) {
        if (optional.isPresent()) {
            Material material = Sheets.getDecoratedPotMaterial(DecoratedPotPatterns.getPatternFromItem((Item)optional.get()));
            if (material != null) {
                return material;
            }
        }
        return Sheets.DECORATED_POT_SIDE;
    }

    public static ChestRenderState.ChestMaterialType getChestMaterial(BlockEntity blockEntity, boolean bl) {
        if (blockEntity instanceof EnderChestBlockEntity) {
            return ChestRenderState.ChestMaterialType.ENDER_CHEST;
        } else if (bl) {
            return ChestRenderState.ChestMaterialType.CHRISTMAS;
        } else if (blockEntity instanceof TrappedChestBlockEntity) {
            return ChestRenderState.ChestMaterialType.TRAPPED;
        } else if (blockEntity.getBlockState().getBlock() instanceof CopperChestBlock copperChestBlock) {
            return switch (copperChestBlock.getState()) {
                case UNAFFECTED -> ChestRenderState.ChestMaterialType.COPPER_UNAFFECTED;
                case EXPOSED -> ChestRenderState.ChestMaterialType.COPPER_EXPOSED;
                case WEATHERED -> ChestRenderState.ChestMaterialType.COPPER_WEATHERED;
                case OXIDIZED -> ChestRenderState.ChestMaterialType.COPPER_OXIDIZED;
            };
        } else {
            return ChestRenderState.ChestMaterialType.REGULAR;
        }
    }
}
