package betterblockentities.client.chunk.pipeline.shelf;

/* local */
import betterblockentities.client.gui.config.ConfigCache;
import betterblockentities.client.gui.option.EnumTypes;
import betterblockentities.mixin.render.immediate.blockentity.shelf.ItemStackRenderStateAccessor;
import betterblockentities.mixin.render.immediate.blockentity.shelf.ItemStackRenderStateLayerAccessor;

/* minecraft */
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.ShelfBlockEntity;

/* fastutil */
import it.unimi.dsi.fastutil.HashCommon;

public final class ShelfItemImmediateFallback {
    private static final ThreadLocal<ItemStackRenderState> RENDER_STATES =
            ThreadLocal.withInitial(ItemStackRenderState::new);

    private ShelfItemImmediateFallback() {}

    public static boolean usesShelfRenderer() {
        return ConfigCache.optimizeShelves && (
                !ConfigCache.optimizeShelfItems
                        || ConfigCache.shelfItemMode == EnumTypes.ShelfItemTypes.FANCY.ordinal()
        );
    }

    public static boolean hasShelfRendererItem(ShelfBlockEntity shelf) {
        for (int slot = 0; slot < shelf.getItems().size(); slot++) {
            if (shouldUseImmediateFallback(shelf, shelf.getItems().get(slot), slot)) {
                return true;
            }
        }

        return false;
    }

    public static boolean shouldUseImmediateFallback(
            ShelfBlockEntity shelf,
            ItemStack stack,
            int slot
    ) {
        if (!usesShelfRenderer() || stack.isEmpty()) return false;

        if (!ConfigCache.optimizeShelfItems) return true;

        if (ConfigCache.shelfItemMode != EnumTypes.ShelfItemTypes.FANCY.ordinal() || !stack.hasFoil())
            return false;

        return hasNonSpecialFoilLayer(shelf, stack, slot);
    }

    private static boolean hasNonSpecialFoilLayer(
            ShelfBlockEntity shelf,
            ItemStack stack,
            int slot
    ) {
        ItemStackRenderState state = RENDER_STATES.get();
        state.clear();

        int seed = HashCommon.long2int(shelf.getBlockPos().asLong()) + slot;

        Minecraft.getInstance().getItemModelResolver().updateForTopItem(
                state,
                stack,
                ItemDisplayContext.ON_SHELF,
                shelf.level(),
                shelf,
                seed
        );

        if (state.isEmpty()) return false;

        ItemStackRenderState.LayerRenderState[] layers = ((ItemStackRenderStateAccessor) state).getLayers();
        int activeLayerCount = ((ItemStackRenderStateAccessor) state).getActiveLayerCount();

        for (int i = 0; i < activeLayerCount; i++) {
            ItemStackRenderStateLayerAccessor layer = (ItemStackRenderStateLayerAccessor) layers[i];
            if (layer.getFoilType() != ItemStackRenderState.FoilType.NONE && layer.getSpecialRenderer() == null) {
                return true;
            }
        }

        return false;
    }
}
