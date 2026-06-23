package betterblockentities.client.chunk.translucent_sorting;

import betterblockentities.client.chunk.pipeline.BBEEmitter;
import org.jspecify.annotations.Nullable;

public interface TranslucentGeometryCollectorExt {
    void bbe$setIncomingQuadSplitMode(BBEEmitter.QuadSplittingMode mode);
    BBEEmitter.QuadSplittingMode bbe$getLastSplitMode();
    void bbe$deferSplittingMode();
}
