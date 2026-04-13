package betterblockentities.mixin.render.immediate.entity;

/* mojang */
import com.mojang.blaze3d.platform.NativeImage;

/* minecraft */
import net.minecraft.client.renderer.texture.SpriteContents;

/* mixin */
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(SpriteContents.class)
public interface SpriteContentsAccessor {
    @Accessor("originalImage")
    NativeImage getOriginalImage();
}
