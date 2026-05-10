package betterblockentities.client.chunk.translucent_sorting;

import betterblockentities.client.chunk.pipeline.BBEEmitter;
import org.jspecify.annotations.Nullable;

public interface TranslucentGeometryCollectorExt {
    void setIncomingQuadSplitMode(BBEEmitter.QuadSplittingMode mode);
    BBEEmitter.QuadSplittingMode getLastSplitMode();
    void deferSplittingMode();
}
