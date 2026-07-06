package betterblockentities.mixin.render.entity;

/* local */
import betterblockentities.client.gui.config.ConfigCache;
import betterblockentities.platform.GlobalScope;

/* minecraft */
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.MinecartRenderer;
import net.minecraft.world.level.block.state.BlockState;

/* mojang */
import com.mojang.blaze3d.vertex.PoseStack;

/* mixin */
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(MinecartRenderer.class)
public abstract class MinecartRendererMixin {
    @WrapOperation(
            method = "renderMinecartContents",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/block/BlockRenderDispatcher;renderSingleBlock(Lnet/minecraft/world/level/block/state/BlockState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V"
            ),
            require = 0
    )
    private void withMinecartDisplayChestBypassContext(
            BlockRenderDispatcher dispatcher,
            BlockState blockState,
            PoseStack poseStack,
            MultiBufferSource multiBufferSource,
            int i,
            int j,
            Operation<Void> original
    ) {
        if (!ConfigCache.optimizeChests) {
            original.call(dispatcher, blockState, poseStack, multiBufferSource, i, j);
            return;
        }

        GlobalScope.isRenderingMinecartDisplay = true;
        try {
            original.call(dispatcher, blockState, poseStack, multiBufferSource, i, j);
        } finally {
            GlobalScope.isRenderingMinecartDisplay = false;
        }
    }
}
