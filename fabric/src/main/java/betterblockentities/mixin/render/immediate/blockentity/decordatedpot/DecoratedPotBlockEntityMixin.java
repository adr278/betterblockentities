package betterblockentities.mixin.render.immediate.blockentity.decordatedpot;

/* local */
import betterblockentities.client.gui.config.ConfigCache;
import betterblockentities.client.render.immediate.blockentity.extentions.BlockEntityExt;
import betterblockentities.client.render.immediate.blockentity.manager.InstancedBlockEntityManager;

/* minecraft */
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.DecoratedPotBlockEntity;

/* mixin */
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DecoratedPotBlockEntity.class)
public class DecoratedPotBlockEntityMixin {
    @Unique private final InstancedBlockEntityManager manager = new InstancedBlockEntityManager((BlockEntity)(Object)this);

    @Inject(method = "<init>", at = @At("TAIL"))
    private void init(CallbackInfo ci) {
        BlockEntity blockEntity = (BlockEntity)(Object)this;
        BlockEntityExt ext = (BlockEntityExt)(Object)blockEntity;

        ext.optKind(InstancedBlockEntityManager.OptKind.POT);

        ext.supportedBlockEntity(
                blockEntity.getType() == BlockEntityType.DECORATED_POT
        );
    }

    @Inject(method = "triggerEvent", at = @At(value = "RETURN", shift = At.Shift.BEFORE, ordinal = 0))
    private void onBlockEvent(int type, int data, CallbackInfoReturnable<Boolean> cir) {
        DecoratedPotBlockEntity decoratedPotBlockEntity = (DecoratedPotBlockEntity)(Object)this;
        BlockEntityExt ext = (BlockEntityExt)(Object)decoratedPotBlockEntity;

        if (ext.supportedBlockEntity() && decoratedPotBlockEntity.lastWobbleStyle != null) {
            manager.trigger(decoratedPotBlockEntity.wobbleStartedAtTick, decoratedPotBlockEntity.lastWobbleStyle.duration, ConfigCache.potAnims);
        }
    }
}
