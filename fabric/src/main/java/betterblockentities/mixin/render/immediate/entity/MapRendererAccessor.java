package betterblockentities.mixin.render.immediate.entity;

/* minecraft */
import net.minecraft.client.renderer.MapRenderer;
import net.minecraft.client.renderer.texture.TextureAtlas;

/* mixin */
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MapRenderer.class)
public interface MapRendererAccessor {
    @Accessor("decorationSprites")
    TextureAtlas getDecorationSprites();
}
