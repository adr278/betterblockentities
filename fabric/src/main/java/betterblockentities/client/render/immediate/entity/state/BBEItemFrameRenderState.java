package betterblockentities.client.render.immediate.entity.state;

/* local */
import betterblockentities.client.chunk.pipeline.itemframe.ItemFrameContentRenderMode;

/* minecraft */
import net.minecraft.client.renderer.entity.state.ItemFrameRenderState;

public class BBEItemFrameRenderState extends ItemFrameRenderState {
    public boolean renderImmediate = true;
    public boolean terrainMeshReady = false;
    public boolean skipBodySubmission = false;
    public boolean skipAllSubmission = false;
    public boolean renderImmediateContents = false;
    public boolean renderMapLabelsFromData = false;
    public int immediateMapLight = 0;
    public ItemFrameContentRenderMode contentRenderMode = ItemFrameContentRenderMode.NONE;
}
