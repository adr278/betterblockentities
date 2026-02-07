package betterblockentities.mixin.render.immediate.blockentity.shelf;

/* minecraft */
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.special.SpecialModelRenderer;

/* annotations */
import org.jetbrains.annotations.Nullable;

/* mixin */
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ItemStackRenderState.LayerRenderState.class)
public interface ItemStackRenderStateLayerAccessor {
    @Accessor("foilType")
    ItemStackRenderState.FoilType getFoilType();

    @Accessor("specialRenderer")
    @Nullable SpecialModelRenderer<Object> getSpecialRenderer();
}
