package betterblockentities.mixin.render.immediate.blockentity.shelf;

/* minecraft */
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;

/* mixin */
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/* java/misc */
import java.util.List;
import java.util.Map;

@Mixin(ModelFeatureRenderer.Storage.class)
public interface ModelFeatureRendererStorageAccessor {
    @Accessor("opaqueModelSubmits") Map<RenderType, List<SubmitNodeStorage.ModelSubmit<?>>>
    getOpaqueModelSubmits();

    @Accessor("translucentModelSubmits") List<SubmitNodeStorage.TranslucentModelSubmit<?>>
    getTranslucentModelSubmits();
}