package betterblockentities.mixin.sodium.pipeline;

/* sodium */
import net.caffeinemc.mods.sodium.client.render.frapi.render.AbstractBlockRenderContext;
import net.caffeinemc.mods.sodium.client.world.LevelSlice;

/* mixin */
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Accessor;

@Pseudo
@Mixin(AbstractBlockRenderContext.class)
public interface AbstractBlockRenderContextAccessor {
    @Accessor("slice")
    LevelSlice getSlice();
}
