package betterblockentities.client.chunk.translucent_sorting;

import betterblockentities.client.chunk.pipeline.BBEEmitter;

public interface TQuadExt {
    void setSplittingMode(BBEEmitter.QuadSplittingMode mode);
    BBEEmitter.QuadSplittingMode getSplittingMode();
}
