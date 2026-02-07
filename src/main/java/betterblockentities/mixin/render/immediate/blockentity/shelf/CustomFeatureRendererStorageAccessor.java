package betterblockentities.mixin.render.immediate.blockentity.shelf;

/* minecraft */
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.CustomFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;

/* mixin */
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/* java/misc */
import java.util.List;
import java.util.Map;

@Mixin(CustomFeatureRenderer.Storage.class)
public interface CustomFeatureRendererStorageAccessor {
    @Accessor("customGeometrySubmits") Map<RenderType, List<SubmitNodeStorage.CustomGeometrySubmit>>
    getCustomGeometrySubmits();
}
