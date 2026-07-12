package betterblockentities.client.chunk.translucent_sorting;

import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.QuadSplittingMode;

public interface TQuadExt {
    void setSplittingMode(QuadSplittingMode mode);

    QuadSplittingMode getSplittingMode();
}
