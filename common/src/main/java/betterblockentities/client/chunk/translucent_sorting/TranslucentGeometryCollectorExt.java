package betterblockentities.client.chunk.translucent_sorting;

public interface TranslucentGeometryCollectorExt {
    void setIncomingQuadSplitMode(QuadSplittingMode mode);

    QuadSplittingMode getLastSplitMode();

    void deferSplittingMode();
}
