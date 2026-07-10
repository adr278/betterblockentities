package betterblockentities.mixin.accessors;

/* sodium */
import net.caffeinemc.mods.sodium.client.render.chunk.RenderSection;

/* mixin */
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(RenderSection.class)
public interface RenderSectionAccessor {

    @Accessor("chunkX")
    int bbe$getChunkX();

    @Accessor("chunkY")
    int bbe$getChunkY();

    @Accessor("chunkZ")
    int bbe$getChunkZ();
}
