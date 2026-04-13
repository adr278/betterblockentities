package betterblockentities.mixin.render.immediate.entity;

/* local */
import betterblockentities.client.render.immediate.entity.renderers.BBEItemFrameRenderer;

/* minecraft */
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ItemFrame;

/* mixin */
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/* java */
import java.util.HashMap;
import java.util.Map;

@Mixin(EntityRenderers.class)
public class EntityRenderersMixin {
    @Inject(method = "createEntityRenderers", at = @At("RETURN"), cancellable = true)
    private static void replaceItemFrameRenderers(
            EntityRendererProvider.Context context,
            CallbackInfoReturnable<Map<EntityType<?>, EntityRenderer<?, ?>>> cir
    ) {
        EntityRendererProvider<ItemFrame> provider = BBEItemFrameRenderer::new;
        HashMap<EntityType<?>, EntityRenderer<?, ?>> renderers = new HashMap<>(cir.getReturnValue());
        renderers.put(EntityType.ITEM_FRAME, provider.create(context));
        renderers.put(EntityType.GLOW_ITEM_FRAME, provider.create(context));
        cir.setReturnValue(Map.copyOf(renderers));
    }
}
