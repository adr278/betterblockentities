package betterblockentities.mixin.sodium.translucent_sorting;

/* local */
import betterblockentities.client.chunk.pipeline.BBEEmitter;
import betterblockentities.client.chunk.translucent_sorting.TQuadExt;

/* sodium */
import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.quad.TQuad;

/* mixin */
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(TQuad.class)
public class TQuadMixin implements TQuadExt {
    @Unique private BBEEmitter.QuadSplittingMode splittingMode;

    @Override
    public void setSplittingMode(BBEEmitter.QuadSplittingMode mode) {
        this.splittingMode = mode;
    }

    @Override
    public BBEEmitter.QuadSplittingMode getSplittingMode() {
        return this.splittingMode;
    }
}
