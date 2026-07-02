package betterblockentities.client.render.immediate.blockentity.renderers;

/* minecraft */
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.HangingSignRenderer;

/**
 * Keeps Hanging Sign registration path on a renderer
 * that matches the vertex-consumer API.
 */
public class BBEHangingSignRenderer extends HangingSignRenderer {
    public BBEHangingSignRenderer(final BlockEntityRendererProvider.Context context) {
        super(context);
    }
}
