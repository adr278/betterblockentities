package betterblockentities.mixin.sodium;

/* local */
import betterblockentities.chunk.BBEDefaultTerrainRenderPasses;

/* minecraft */
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.resources.Identifier;

/* sodium */
import net.caffeinemc.mods.sodium.client.gl.shader.*;
import net.caffeinemc.mods.sodium.client.render.chunk.ShaderChunkRenderer;
import net.caffeinemc.mods.sodium.client.render.chunk.shader.ChunkShaderOptions;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.TerrainRenderPass;

/* local */
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

/* see DefaultTerrainRenderPasses mixin for more context to as why we are doing this */
@Pseudo
@Mixin(ShaderChunkRenderer.class)
public abstract class ShaderChunkRendererMixin {
    @WrapOperation(method = "createShader", at = @At(value = "INVOKE", target =
            "Lnet/caffeinemc/mods/sodium/client/gl/shader/ShaderLoader;" +
            "loadShader(" +
            "Lnet/caffeinemc/mods/sodium/client/gl/shader/ShaderType;" +
            "Lnet/minecraft/resources/Identifier;" +
            "Lnet/caffeinemc/mods/sodium/client/gl/shader/ShaderConstants;" +
            ")Lnet/caffeinemc/mods/sodium/client/gl/shader/GlShader;"
        )
    )
    private GlShader wrapLoadShader(ShaderType type, Identifier id, ShaderConstants constants, Operation<GlShader> original, @Local(argsOnly = true) ChunkShaderOptions options) {
        if (shouldReplace(options)) {
            return original.call(type, getBBEShader(type), constants);
        }
        return original.call(type, id, constants);
    }

    @Unique
    private boolean shouldReplace(ChunkShaderOptions options) {
        TerrainRenderPass pass =  options.pass();
        return pass == BBEDefaultTerrainRenderPasses.SOLID        ||
                pass == BBEDefaultTerrainRenderPasses.CUTOUT      ||
                pass == BBEDefaultTerrainRenderPasses.TRANSLUCENT;
    }

    @Unique
    private Identifier getBBEShader(ShaderType type) {
        return type == ShaderType.VERTEX
                ? Identifier.fromNamespaceAndPath("betterblockentities", "blocks/block_layer_opaque.vsh")
                : Identifier.fromNamespaceAndPath("betterblockentities", "blocks/block_layer_opaque.fsh");
    }
}
