package betterblockentities.mixin.minecraft.sign;

import net.minecraft.client.model.Model;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(targets = "net.minecraft.client.renderer.blockentity.SignRenderer$Models")
public interface SignRendererModelsAccessor {
    @Invoker("standing")
    Model.Simple invokeStanding();

    @Invoker("wall")
    Model.Simple invokeWall();
}
