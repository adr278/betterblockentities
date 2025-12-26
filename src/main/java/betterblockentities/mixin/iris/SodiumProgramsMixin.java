package betterblockentities.mixin.iris;

/* local */
import betterblockentities.chunk.BBEDefaultTerrainRenderPasses;

/* sodium */
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.DefaultTerrainRenderPasses;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.TerrainRenderPass;

/* iris */
import net.irisshaders.iris.pipeline.programs.SodiumPrograms;
import net.irisshaders.iris.shadows.ShadowRenderingState;

/* mixin */
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Pseudo;

@Pseudo
@Mixin(SodiumPrograms.class)
public class SodiumProgramsMixin {
    /** maybe not compleatly overwrite this? */
    @Overwrite
    private SodiumPrograms.Pass mapTerrainRenderPass(TerrainRenderPass pass) {
        if (pass == DefaultTerrainRenderPasses.SOLID || pass == BBEDefaultTerrainRenderPasses.SOLID) {
            return ShadowRenderingState.areShadowsCurrentlyBeingRendered() ? SodiumPrograms.Pass.SHADOW : SodiumPrograms.Pass.TERRAIN;
        } else if (pass == DefaultTerrainRenderPasses.CUTOUT || pass == BBEDefaultTerrainRenderPasses.CUTOUT) {
            return ShadowRenderingState.areShadowsCurrentlyBeingRendered() ? SodiumPrograms.Pass.SHADOW_CUTOUT : SodiumPrograms.Pass.TERRAIN_CUTOUT;
        } else if (pass == DefaultTerrainRenderPasses.TRANSLUCENT || pass == BBEDefaultTerrainRenderPasses.TRANSLUCENT) {
            return ShadowRenderingState.areShadowsCurrentlyBeingRendered() ? SodiumPrograms.Pass.SHADOW_TRANS : SodiumPrograms.Pass.TRANSLUCENT;
        }
        else {
            throw new IllegalArgumentException("Unknown pass: " + String.valueOf(pass));
        }
    }
}
