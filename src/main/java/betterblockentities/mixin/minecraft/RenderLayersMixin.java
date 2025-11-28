package betterblockentities.mixin.minecraft;

/* minecraft */
import betterblockentities.gui.ConfigManager;


/* mixin */
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemBlockRenderTypes.class)
public class RenderLayersMixin {
    /* force some of the supported blocks to the TRANSLUCENT renderlayer inorder to get proper rendering */
    @Inject(method = "getChunkRenderType", at = @At("HEAD"), cancellable = true)
    private static void forceBlockLayer(BlockState state, CallbackInfoReturnable<ChunkSectionLayer> cir) {
        if (!ConfigManager.CONFIG.master_optimize) return;
        Block block = state.getBlock();
        if (block instanceof ShulkerBoxBlock ||
                block instanceof DecoratedPotBlock ||
                block instanceof CeilingHangingSignBlock ||
                block instanceof WallHangingSignBlock)
            cir.setReturnValue(ChunkSectionLayer.TRANSLUCENT);
    }
}

