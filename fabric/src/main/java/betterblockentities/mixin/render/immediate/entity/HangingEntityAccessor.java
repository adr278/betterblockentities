package betterblockentities.mixin.render.immediate.entity;

/* minecraft */
import net.minecraft.core.Direction;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.decoration.HangingEntity;

/* mixin */
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(HangingEntity.class)
public interface HangingEntityAccessor {
    @Accessor("DATA_DIRECTION")
    static EntityDataAccessor<Direction> getDataDirection() { throw new AssertionError(); }
}
