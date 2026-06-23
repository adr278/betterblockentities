package betterblockentities.client.chunk.translucent_sorting;

import betterblockentities.client.chunk.pipeline.BBEEmitter;

public interface TQuadExt {
    void bbe$setSplittingMode(BBEEmitter.QuadSplittingMode mode);
    BBEEmitter.QuadSplittingMode bbe$getSplittingMode();
}
