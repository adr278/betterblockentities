package betterblockentities.mixin.sodium.translucent_sorting;

/* local */
import betterblockentities.client.chunk.pipeline.BBEEmitter;
import betterblockentities.client.chunk.translucent_sorting.TQuadExt;

/* sodium */
import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.quad.FullTQuad;

/* java/misc */
import it.unimi.dsi.fastutil.ints.IntArrayList;
import org.joml.Vector3fc;

/* mixin */
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

@Mixin(targets = "net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.bsp_tree.InnerPartitionBSPNode")
public class InnerPartitionBSPNodeMixin {
    @WrapOperation(method = "handleUnsortableBySplitting",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/caffeinemc/mods/sodium/client/render/chunk/translucent_sorting/bsp_tree/InnerPartitionBSPNode;splitCandidate(Lnet/caffeinemc/mods/sodium/client/render/chunk/translucent_sorting/bsp_tree/BSPWorkspace;Lit/unimi/dsi/fastutil/ints/IntArrayList;ILnet/caffeinemc/mods/sodium/client/render/chunk/translucent_sorting/quad/FullTQuad;Lorg/joml/Vector3fc;FLit/unimi/dsi/fastutil/ints/IntArrayList;Lit/unimi/dsi/fastutil/ints/IntArrayList;)V"
            )
    )
    private static void skipSplitForTaggedQuads(
            @Coerce Object workspace,
            IntArrayList splittingGroup,
            int candidateIndex, FullTQuad insideQuad,
            Vector3fc splitPlane,
            float splitDistance,
            IntArrayList outside,
            IntArrayList inside,
            Operation<Void> original
    ) {
        TQuadExt tQuadExt = (TQuadExt)insideQuad;
        if (tQuadExt.getSplittingMode() == BBEEmitter.QuadSplittingMode.NONE) {
            splittingGroup.add(candidateIndex);
            return;
        }

        original.call(workspace, splittingGroup, candidateIndex, insideQuad, splitPlane, splitDistance, outside, inside);
    }
}
