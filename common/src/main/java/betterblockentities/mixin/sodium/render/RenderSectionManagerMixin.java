package betterblockentities.mixin.sodium.render;

/* local */
import betterblockentities.client.chunk.section.SectionRebuildCallbacks;
import betterblockentities.mixin.accessors.RenderSectionAccessor;

/* sodium */
import net.caffeinemc.mods.sodium.client.render.chunk.RenderSection;
import net.caffeinemc.mods.sodium.client.render.chunk.RenderSectionManager;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.BuilderTaskOutput;

/* mixin */
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(RenderSectionManager.class)
public class RenderSectionManagerMixin {
    @Redirect(method = "processChunkBuilds", at = @At(value = "INVOKE", target = "Lnet/caffeinemc/mods/sodium/client/render/chunk/compile/BuilderTaskOutput;destroy()V"), remap = false, require = 1)
    private void bbe$callback(BuilderTaskOutput out) {
        out.destroy();

        if (SectionRebuildCallbacks.isEmpty()) {
            return;
        }

        RenderSection section = out.section;
        RenderSectionAccessor accessor = (RenderSectionAccessor) section;

        long key = SectionRebuildCallbacks.keyFromSectionPos(accessor.bbe$getChunkX(), accessor.bbe$getChunkY(), accessor.bbe$getChunkZ());
        SectionRebuildCallbacks.fireIfWaiting(key);
    }
}
