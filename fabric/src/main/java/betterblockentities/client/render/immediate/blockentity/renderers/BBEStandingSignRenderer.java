package betterblockentities.client.render.immediate.blockentity.renderers;

/* minecraft */
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.SignRenderer;

/**
 * Keeps Standing Sign registration path on a renderer
 * that matches the vertex-consumer API.
 */
public class BBEStandingSignRenderer extends SignRenderer {
    public BBEStandingSignRenderer(final BlockEntityRendererProvider.Context context) {
        super(context);
    }
}
