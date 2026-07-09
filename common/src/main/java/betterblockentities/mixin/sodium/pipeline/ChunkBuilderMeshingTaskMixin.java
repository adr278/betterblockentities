package betterblockentities.mixin.sodium.pipeline;

/* local */
import betterblockentities.client.chunk.pipeline.BBEBlockRenderer;
import betterblockentities.client.render.immediate.blockentity.extentions.BlockEntityExt;

/* minecraft */
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/* sodium */
import net.caffeinemc.mods.sodium.client.render.chunk.compile.tasks.ChunkBuilderMeshingTask;
import net.caffeinemc.mods.sodium.client.world.LevelSlice;

/* mixin */
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

@Pseudo
@Mixin(ChunkBuilderMeshingTask.class)
public abstract class ChunkBuilderMeshingTaskMixin {
    @WrapOperation(
            method = "execute(Lnet/caffeinemc/mods/sodium/client/render/chunk/compile/ChunkBuildContext;Lnet/caffeinemc/mods/sodium/client/util/task/CancellationToken;)Lnet/caffeinemc/mods/sodium/client/render/chunk/compile/ChunkBuildOutput;",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/state/BlockState;getRenderShape()Lnet/minecraft/world/level/block/RenderShape;"
            )
    )
    private RenderShape useModelForSupportedBlockEntities(
            BlockState blockState,
            Operation<RenderShape> original,
            @Local(name = "slice") LevelSlice slice,
            @Local(name = "blockPos") BlockPos.MutableBlockPos blockPos
    ) {
        final RenderShape renderShape = original.call(blockState);
        if (renderShape == RenderShape.MODEL || !blockState.hasBlockEntity()) {
            return renderShape;
        }

        final BlockEntity blockEntity = BBEBlockRenderer.tryGetBlockEntity(slice, blockPos);
        if (blockEntity == null || !(blockEntity instanceof BlockEntityExt ext) || !ext.supportedBlockEntity()) {
            return RenderShape.INVISIBLE;
        }

        return RenderShape.MODEL;
    }
}
