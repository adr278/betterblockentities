package betterblockentities.client.chunk.pipeline.itemframe;

public final class MapAtlasBudgetPlanner {
    public static final int PAGE_SIZE = 128;
    public static final int PAGE_GUTTER = 1;
    public static final int SLOT_STRIDE = PAGE_SIZE + PAGE_GUTTER * 2;
    public static final int DEFAULT_SLOTS_PER_AXIS = 32;
    public static final int DEFAULT_ATLAS_EDGE_LIMIT = DEFAULT_SLOTS_PER_AXIS * SLOT_STRIDE;

    public record BudgetResult(
            int exactFit,
            int safeBudget,
            int maxTextureSize,
            int atlasWidth,
            int atlasHeight
    ) {}

    private MapAtlasBudgetPlanner() {}

    public static BudgetResult computeBudget(int maxTextureSize, int atlasEdgeLimit) {
        int atlasEdgeCap = atlasEdgeLimit > 0 ? atlasEdgeLimit : DEFAULT_ATLAS_EDGE_LIMIT;
        int atlasEdge = Math.min(maxTextureSize, atlasEdgeCap);
        atlasEdge -= atlasEdge % SLOT_STRIDE;

        if (atlasEdge < SLOT_STRIDE) {
            return new BudgetResult(0, 0, maxTextureSize, 0, 0);
        }

        int slotsPerAxis = atlasEdge / SLOT_STRIDE;
        int exactFit = slotsPerAxis * slotsPerAxis;

        return new BudgetResult(
                exactFit,
                exactFit,
                maxTextureSize,
                atlasEdge,
                atlasEdge
        );
    }
}
