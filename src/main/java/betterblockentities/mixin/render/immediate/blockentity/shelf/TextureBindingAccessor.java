package betterblockentities.mixin.render.immediate.blockentity.shelf;

/* minecraft */
import net.minecraft.resources.Identifier;

/* mixin */
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(targets = "net.minecraft.client.renderer.rendertype.RenderSetup$TextureBinding")
public interface TextureBindingAccessor {
    @Accessor("location") Identifier
    GetLocation();
}
