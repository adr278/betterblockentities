package betterblockentities.mixin.core;

/* local */
import betterblockentities.client.render.immediate.blockentity.extentions.BlockEntityExt;
import betterblockentities.client.render.immediate.blockentity.misc.LidControllerSync;

/* minecraft */
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.BlockDestructionProgress;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/* mixin */
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/* java/misc */
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import java.util.SortedSet;

@Mixin(ClientLevel.class)
public class ClientLevelMixin {
    @Shadow @Final private Long2ObjectMap<SortedSet<BlockDestructionProgress>> destructionProgress;

    /**
     * Synchronizes the new chest half after its block entity is created but before
     * the renderer is notified, preventing it from briefly rendering as a closed chest.
     */
    @Inject(method = "setBlocksDirty", at = @At("HEAD"))
    private void bbe$syncLidControllerBeforeRenderUpdate(BlockPos blockPos, BlockState oldState, BlockState newState, CallbackInfo ci) {
        LidControllerSync.sync((ClientLevel)(Object)this, blockPos, newState);
    }

    @Inject(method = "setServerVerifiedBlockState", at = @At("TAIL"))
    public void bbe$syncLidControllers(BlockPos blockPos, BlockState blockState, int i, CallbackInfo ci) {
        LidControllerSync.sync((ClientLevel)(Object)this, blockPos, blockState);
    }

    @Inject(method = "destroyBlockProgress", at = @At("TAIL"))
    private void bbe$markBreakingOverlay(int breakerId, BlockPos blockPos, int progress, CallbackInfo ci) {
        if (progress >= 0 && progress < 10) {
            this.bbe$setBreakingOverlay(blockPos, true);
        }
    }

    @Inject(method = "removeProgress", at = @At("TAIL"))
    private void bbe$refreshBreakingOverlay(BlockDestructionProgress progress, CallbackInfo ci) {
        BlockPos blockPos = progress.getPos();
        this.bbe$setBreakingOverlay(blockPos, this.destructionProgress.containsKey(blockPos.asLong()));
    }

    @Unique
    private void bbe$setBreakingOverlay(BlockPos blockPos, boolean breakingOverlay) {
        BlockEntity blockEntity = ((ClientLevel)(Object)this).getBlockEntity(blockPos);
        if (blockEntity != null) {
            ((BlockEntityExt)blockEntity).bbe$setBreakingOverlay(breakingOverlay);
        }
    }
}
