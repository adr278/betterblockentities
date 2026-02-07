package betterblockentities.mixin.sodium.pipeline;

/* minecraft */
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

/* sodium */
import net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderer;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.TerrainRenderPass;

/* mixin */
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Invoker;

@Pseudo @Mixin(BlockRenderer.class)
public interface BlockRendererInvoker {
    @Invoker("getDowngradedPass")
    @SuppressWarnings("unused") static TerrainRenderPass invokeGetDowngradedPass(TextureAtlasSprite sprite, TerrainRenderPass pass) {
        throw new AssertionError("Mixin Invoker not applied");
    }
}
