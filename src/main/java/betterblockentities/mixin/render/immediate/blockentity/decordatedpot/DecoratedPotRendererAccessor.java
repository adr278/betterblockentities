package betterblockentities.mixin.render.immediate.blockentity.decordatedpot;

import net.minecraft.client.renderer.blockentity.DecoratedPotRenderer;
import net.minecraft.client.renderer.blockentity.state.ChestRenderState;
import net.minecraft.client.resources.model.Material;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.Optional;

@Mixin(DecoratedPotRenderer.class)
public interface DecoratedPotRendererAccessor {
    @Invoker("getSideMaterial")
    Material getSideMaterialInvoke(Optional<Item> optional);
}
