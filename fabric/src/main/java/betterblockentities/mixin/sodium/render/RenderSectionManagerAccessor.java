package betterblockentities.mixin.sodium.render;

/* sodium */
import net.caffeinemc.mods.sodium.client.render.chunk.RenderSection;
import net.caffeinemc.mods.sodium.client.render.chunk.RenderSectionManager;

/* mixin */
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(RenderSectionManager.class)
public interface RenderSectionManagerAccessor {
    @Invoker("getRenderSection")
    RenderSection invokeGetRenderSection(int sectionX, int sectionY, int sectionZ);
}
