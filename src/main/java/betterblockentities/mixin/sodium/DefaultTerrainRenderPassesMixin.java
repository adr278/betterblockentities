package betterblockentities.mixin.sodium;

/* local */
import betterblockentities.chunk.BBEDefaultTerrainRenderPasses;

/* sodium */
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.DefaultTerrainRenderPasses;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.TerrainRenderPass;

/* mixin */
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/*
    inject our own render passes so we can bind our shaders. we need to modify
    Sodium's core fragment shader to solve some issues caused by how our entity
    textures gets stitched in the block atlas (causes weird sampling/artifacts
    upon texture filtering / blending) this is fixable with some adjustments to
    the fragment shader that runs on the terrain geometry
*/

@Pseudo
@Mixin(DefaultTerrainRenderPasses.class)
public abstract class DefaultTerrainRenderPassesMixin {
    @Shadow @Final @Mutable public static TerrainRenderPass[] ALL;

    @Inject(method = "<clinit>", at = @At("TAIL"))
    private static void addBBERenderPasses(CallbackInfo ci) {
        TerrainRenderPass[] original = ALL;
        TerrainRenderPass[] toAdd = {BBEDefaultTerrainRenderPasses.SOLID, BBEDefaultTerrainRenderPasses.CUTOUT, BBEDefaultTerrainRenderPasses.TRANSLUCENT};

        TerrainRenderPass[] extended = new TerrainRenderPass[original.length + toAdd.length];

        for (int i = 0; i < original.length; i++) {
            extended[i] = original[i];
        }

        for (int i = 0; i < toAdd.length; i++) {
            extended[original.length + i] = toAdd[i];
        }

        ALL = extended;
    }
}
