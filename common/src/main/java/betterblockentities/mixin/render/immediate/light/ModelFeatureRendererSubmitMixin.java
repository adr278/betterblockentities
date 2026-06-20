package betterblockentities.mixin.render.immediate.light;

/* local */
import betterblockentities.client.render.immediate.light.ImmediateBlockEntityLight;
import betterblockentities.client.render.immediate.light.ImmediateLightSubmitExt;

/* minecraft */
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;

/* mixin */
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ModelFeatureRenderer.Submit.class)
public class ModelFeatureRendererSubmitMixin implements ImmediateLightSubmitExt {
    @Unique private ImmediateBlockEntityLight.Parameters bbe$lightParameters;

    @Override
    public void bbe$setLightParameters(ImmediateBlockEntityLight.Parameters parameters) {
        this.bbe$lightParameters = parameters;
    }

    @Override
    public ImmediateBlockEntityLight.Parameters bbe$getLightParameters() {
        return this.bbe$lightParameters;
    }
}
