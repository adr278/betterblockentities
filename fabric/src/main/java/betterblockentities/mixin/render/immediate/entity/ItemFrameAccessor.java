package betterblockentities.mixin.render.immediate.entity;

/* minecraft */
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.ItemStack;

/* mixin */
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ItemFrame.class)
public interface ItemFrameAccessor {
    @Accessor("DATA_ITEM")
    static EntityDataAccessor<ItemStack> getDataItem() { throw new AssertionError(); }

    @Accessor("DATA_ROTATION")
    static EntityDataAccessor<Integer> getDataRotation() { throw new AssertionError(); }
}
