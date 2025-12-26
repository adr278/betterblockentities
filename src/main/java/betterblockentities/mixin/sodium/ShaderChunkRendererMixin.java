package betterblockentities.mixin.sodium;

/* local */
import betterblockentities.chunk.BBEDefaultTerrainRenderPasses;

/* minecraft */
import net.minecraft.resources.Identifier;

/* sodium */
import net.caffeinemc.mods.sodium.client.gl.shader.*;
import net.caffeinemc.mods.sodium.client.render.chunk.ShaderChunkRenderer;
import net.caffeinemc.mods.sodium.client.render.chunk.shader.ChunkShaderInterface;
import net.caffeinemc.mods.sodium.client.render.chunk.shader.ChunkShaderOptions;
import net.caffeinemc.mods.sodium.client.render.chunk.shader.DefaultShaderInterface;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.TerrainRenderPass;

/* local */
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/* see DefaultTerrainRenderPasses mixin for more context to as why we are doing this */

@Pseudo
@Mixin(ShaderChunkRenderer.class)
public abstract class ShaderChunkRendererMixin {
    @Inject(method = "createShader", at = @At("HEAD"), cancellable = true)
    private void loadBBECoreShaders(String path, ChunkShaderOptions options, CallbackInfoReturnable<GlProgram<ChunkShaderInterface>> cir) {
        /* load our custom shaders for our passes */
        TerrainRenderPass pass = options.pass();
        if (pass == BBEDefaultTerrainRenderPasses.SOLID       ||
            pass == BBEDefaultTerrainRenderPasses.CUTOUT      ||
            pass == BBEDefaultTerrainRenderPasses.TRANSLUCENT
        ) {
            ShaderConstants constants = ((ShaderChunkRendererAccessor)this).createShaderConstantsInvoke(options);
            GlShader vertShader = ShaderLoader.loadShader(ShaderType.VERTEX, Identifier.fromNamespaceAndPath("betterblockentities", path + ".vsh"), constants);
            GlShader fragShader = ShaderLoader.loadShader(ShaderType.FRAGMENT, Identifier.fromNamespaceAndPath("betterblockentities", path + ".fsh"), constants);

            GlProgram program;
            try {
                program = GlProgram.builder(Identifier.fromNamespaceAndPath("betterblockentities", "chunk_shader")).attachShader(vertShader).attachShader(fragShader).bindAttribute("a_Position", 0).bindAttribute("a_Color", 1).bindAttribute("a_TexCoord", 2).bindAttribute("a_LightAndData", 3).bindFragmentData("fragColor", 0).link((shader) -> new DefaultShaderInterface(shader, options));
            } finally {
                vertShader.delete();
                fragShader.delete();
            }
            cir.setReturnValue(program);
            cir.cancel();
        }

    }
}
