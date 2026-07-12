package betterblockentities.client.chunk.translucent_sorting;

import betterblockentities.client.chunk.pipeline.BBEEmitter;
import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.QuadSplittingMode;
import org.jspecify.annotations.Nullable;

public interface TranslucentGeometryCollectorExt {
    void bbe$setIncomingQuadSplitMode(QuadSplittingMode mode);
    QuadSplittingMode bbe$getLastSplitMode();
    void bbe$deferSplittingMode();
}
