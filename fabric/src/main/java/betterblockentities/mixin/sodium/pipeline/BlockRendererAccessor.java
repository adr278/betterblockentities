package betterblockentities.mixin.sodium.pipeline;

/* sodium */
import net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderer;

/* joml */
import org.joml.Vector3f;

/* mixin */
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Accessor;

@Pseudo
@Mixin(BlockRenderer.class)
public interface BlockRendererAccessor {
    @Accessor("posOffset")
    Vector3f getPosOffset();
}
