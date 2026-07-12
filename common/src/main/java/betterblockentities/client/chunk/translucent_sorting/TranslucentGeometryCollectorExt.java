package betterblockentities.client.chunk.translucent_sorting;

import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.QuadSplittingMode;

public interface TranslucentGeometryCollectorExt {
    void setIncomingQuadSplitMode(QuadSplittingMode mode);

    QuadSplittingMode getLastSplitMode();

    void deferSplittingMode();
}
