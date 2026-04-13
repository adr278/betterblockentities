package betterblockentities.mixin.render.immediate.entity;

/* mojang */
import com.mojang.blaze3d.platform.NativeImage;

/* mixin */
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(NativeImage.class)
public interface NativeImageAccessor {
    @Accessor("pixels")
    long getPixels();
}
