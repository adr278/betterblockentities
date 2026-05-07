package betterblockentities.mixin.render.immediate.blockentity.bell;

/* local */
import betterblockentities.client.gui.config.ConfigCache;
import betterblockentities.client.render.immediate.blockentity.extentions.BlockEntityExt;
import betterblockentities.client.render.immediate.blockentity.manager.InstancedBlockEntityManager;

/* minecraft */
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BellBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/* mixin */
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BellBlockEntity.class)
public class BellBlockEntityMixin {
    @Unique private final InstancedBlockEntityManager manager = new InstancedBlockEntityManager((BlockEntity)(Object)this);

    @Inject(method = "<init>", at = @At("TAIL"))
    private void init(CallbackInfo ci) {
        BlockEntity blockEntity = (BlockEntity)(Object)this;
        BlockEntityExt ext = (BlockEntityExt)(Object)this;

        ext.optKind(InstancedBlockEntityManager.OptKind.BELL);

        ext.supportedBlockEntity(
                blockEntity.getType() == BlockEntityType.BELL
        );
    }

    @Inject(method = "clientTick", at = @At("TAIL"))
    private static void onTick(Level level, BlockPos blockPos, BlockState blockState, BellBlockEntity bellBlockEntity, CallbackInfo ci) {
        BellBlockEntityMixin self = (BellBlockEntityMixin)(Object)bellBlockEntity;
        BlockEntityExt ext = (BlockEntityExt)(Object)bellBlockEntity;

        if (ext.supportedBlockEntity()) {
            self.manager.tick(bellBlockEntity.shaking, ConfigCache.bellAnims);
        }
    }
}
