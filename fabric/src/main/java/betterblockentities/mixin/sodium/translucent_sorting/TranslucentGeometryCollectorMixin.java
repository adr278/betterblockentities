package betterblockentities.mixin.sodium.translucent_sorting;

/* local */
import betterblockentities.client.chunk.translucent_sorting.QuadSplittingMode;
import betterblockentities.client.chunk.translucent_sorting.TQuadExt;
import betterblockentities.client.chunk.translucent_sorting.TranslucentGeometryCollectorExt;

/* sodium */
import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.TranslucentGeometryCollector;
import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.quad.TQuad;

/* mixin */
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(TranslucentGeometryCollector.class)
public class TranslucentGeometryCollectorMixin implements TranslucentGeometryCollectorExt {
    @Unique private QuadSplittingMode lastSplittingMode = QuadSplittingMode.DEFERRED;

    @WrapOperation(
            method = "appendQuad",
            at = @At(
                    value = "INVOKE",
                    target = "Lit/unimi/dsi/fastutil/objects/ReferenceArrayList;add(Ljava/lang/Object;)Z"
            )
    )
    private boolean appendQuad(
            final ReferenceArrayList<?> instance,
            final Object appendingQuad,
            final Operation<Boolean> original
    ) {
        final TQuad tsQuad = (TQuad) appendingQuad;
        if (this.lastSplittingMode != QuadSplittingMode.DEFERRED) {
            ((TQuadExt) tsQuad).setSplittingMode(this.lastSplittingMode);
        }
        return original.call(instance, appendingQuad);
    }

    @Override public void setIncomingQuadSplitMode(final QuadSplittingMode mode) {
        this.lastSplittingMode = mode;
    }

    @Override public QuadSplittingMode getLastSplitMode() {
        return this.lastSplittingMode;
    }

    @Override public void deferSplittingMode() {
        this.lastSplittingMode = QuadSplittingMode.DEFERRED;
    }
}
