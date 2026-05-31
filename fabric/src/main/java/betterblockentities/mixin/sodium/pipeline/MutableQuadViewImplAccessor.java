package betterblockentities.mixin.sodium.pipeline;

/* sodium */
import net.caffeinemc.mods.sodium.client.render.frapi.mesh.MutableQuadViewImpl;

/* mixin */
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(MutableQuadViewImpl.class)
public interface MutableQuadViewImplAccessor {
    @Invoker("fromVanillaInternal")
    void fromVanillaInternalInvoke(int[] quadData, int startIndex);
}
