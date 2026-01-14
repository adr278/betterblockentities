package betterblockentities.mixin.render.immediate.blockentity.chest;

import net.minecraft.client.renderer.blockentity.ChestRenderer;
import net.minecraft.client.renderer.blockentity.state.ChestRenderState;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ChestRenderer.class)
public interface ChestRendererAccessor {
    @Invoker("getChestMaterial")
    ChestRenderState.ChestMaterialType getChestMaterialInvoke(BlockEntity blockEntity, boolean bl);
}
