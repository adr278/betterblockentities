package betterblockentities.mixin.render.immediate;

/* minecraft */
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;

/* mixin */
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/* java/misc */
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import it.unimi.dsi.fastutil.ints.IntArrays;

@Mixin(ModelFeatureRenderer.class)
public class ModelFeatureRendererMixin {

    @Redirect(method = "render(Lnet/minecraft/client/renderer/SubmitNodeCollection;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;Lnet/minecraft/client/renderer/OutlineBufferSource;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/List;sort(Ljava/util/Comparator;)V"
            )
    )
    private void arrayIntSort(List<SubmitNodeStorage.TranslucentModelSubmit<?>> list, Comparator<?> comparator) {
        int size = list.size();
        if (size < 2)
            return;

        double[] keys = new double[size];
        int[] indices = new int[size];

        for (int i = 0; i < size; i++) {
            keys[i] = -list.get(i).position().lengthSquared();
            indices[i] = i;
        }

        IntArrays.quickSort(indices, (a, b) -> {
            int c = Double.compare(keys[a], keys[b]);
            return c == 0 ? Integer.compare(a, b) : c;
        });

        List<SubmitNodeStorage.TranslucentModelSubmit<?>> sorted = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            sorted.add(list.get(indices[i]));
        }

        list.clear();
        list.addAll(sorted);
    }
}