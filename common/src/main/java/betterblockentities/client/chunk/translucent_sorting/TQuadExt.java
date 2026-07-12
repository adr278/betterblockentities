package betterblockentities.client.chunk.translucent_sorting;

import betterblockentities.client.chunk.pipeline.BBEEmitter;
import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.QuadSplittingMode;

public interface TQuadExt {
    void bbe$setSplittingMode(QuadSplittingMode mode);
    QuadSplittingMode bbe$getSplittingMode();
}
