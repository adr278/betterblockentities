package betterblockentities.mixin.sodium;

/* sodium */
import net.caffeinemc.mods.sodium.client.gl.shader.ShaderConstants;
import net.caffeinemc.mods.sodium.client.render.chunk.ShaderChunkRenderer;
import net.caffeinemc.mods.sodium.client.render.chunk.shader.ChunkShaderOptions;

/* mixin */
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Invoker;

@Pseudo
@Mixin(ShaderChunkRenderer.class)
public interface ShaderChunkRendererAccessor {
    @Invoker("createShaderConstants")
    ShaderConstants createShaderConstantsInvoke(ChunkShaderOptions options);
}
