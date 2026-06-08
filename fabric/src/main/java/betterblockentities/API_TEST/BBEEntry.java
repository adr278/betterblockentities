package betterblockentities.API_TEST;

import betterblockentities.data.SupportedBlockEntityTypes;
import betterblockentities.registration.AltRendererRegistration;
import betterblockentities.registration.BBEApiEntryPoint;

public class BBEEntry implements BBEApiEntryPoint {
    @Override
    public void registerRenderers(AltRendererRegistration context) {
        context.registerRenderer(SupportedBlockEntityTypes.CHEST, ApiTestRenderer::new);
    }
}
