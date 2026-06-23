package betterblockentities.mixin.accessors;

import net.minecraft.world.level.block.entity.ChestLidController;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ChestLidController.class)
public interface ChestLidControllerAccessor {
    @Accessor("shouldBeOpen")
    boolean bbe$getOpen();

    @Accessor("shouldBeOpen")
    void bbe$setOpen(boolean value);

    @Accessor("openness")
    float bbe$getProgress();

    @Accessor("openness")
    void bbe$setProgress(float value);

    @Accessor("oOpenness")
    float bbe$getLastProgress();

    @Accessor("oOpenness")
    void bbe$setLastProgress(float value);
}
