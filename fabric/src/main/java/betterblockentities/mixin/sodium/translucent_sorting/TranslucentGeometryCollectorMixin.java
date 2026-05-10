package betterblockentities.mixin.sodium.translucent_sorting;

/* local */
import betterblockentities.client.chunk.pipeline.BBEEmitter;
import betterblockentities.client.chunk.translucent_sorting.TQuadExt;
import betterblockentities.client.chunk.translucent_sorting.TranslucentGeometryCollectorExt;

/* sodium */
import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.TranslucentGeometryCollector;
import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.quad.TQuad;

/* mixin */
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

/* java/misc */
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;

@Mixin(TranslucentGeometryCollector.class)
public class TranslucentGeometryCollectorMixin implements TranslucentGeometryCollectorExt {
    @Unique private BBEEmitter.QuadSplittingMode lastSplittingMode;

    @WrapOperation(method = "appendQuad",
            at = @At(
                    value = "INVOKE",
                    target = "it/unimi/dsi/fastutil/objects/ReferenceArrayList.add(Ljava/lang/Object;)Z"
            )
    )
    public boolean appendQuad(ReferenceArrayList<?> instance, Object appendingQuad, Operation<Boolean> original) {
        TQuad tsQuad = (TQuad)appendingQuad;

        if (getLastSplitMode() != BBEEmitter.QuadSplittingMode.DEFERRED) {
            TQuadExt tQuadExt = (TQuadExt)tsQuad;
            tQuadExt.setSplittingMode(getLastSplitMode());
        }
        return original.call(instance, appendingQuad);
    }

    @Override
    public void setIncomingQuadSplitMode(BBEEmitter.QuadSplittingMode mode) {
        this.lastSplittingMode = mode;
    }

    @Override
    public BBEEmitter.QuadSplittingMode getLastSplitMode() {
        return this.lastSplittingMode;
    }

    @Override
    public void deferSplittingMode() {
        this.lastSplittingMode = BBEEmitter.QuadSplittingMode.DEFERRED;
    }
}
