package betterblockentities.mixin.render.immediate.blockentity.sign;

/* local */
import betterblockentities.client.render.immediate.blockentity.extentions.BlockEntityExt;
import betterblockentities.client.render.immediate.blockentity.extentions.SignBlockEntityExt;
import betterblockentities.client.render.immediate.blockentity.manager.InstancedBlockEntityManager;
import betterblockentities.client.render.immediate.blockentity.manager.SpecialBlockEntityManager;

/* minecraft */
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;

/* mixin */
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SignBlockEntity.class)
public class SignBlockEntityMixin implements SignBlockEntityExt {
    @Unique private SignText cachedFrontText;
    @Unique private SignText cachedBackText;
    @Unique private boolean cachedHasAnyText;

    @Inject(method = "<init>(Lnet/minecraft/world/level/block/entity/BlockEntityType;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)V", at = @At("TAIL"))
    private void init(CallbackInfo ci) {
        BlockEntity blockEntity = (BlockEntity)(Object)this;
        BlockEntityExt ext = (BlockEntityExt)(Object)this;

        ext.terrainMeshReady(true);
        ext.hasSpecialManager(true);
        ext.optKind(InstancedBlockEntityManager.OptKind.SIGN);

        ext.supportedBlockEntity(
                blockEntity.getType() == BlockEntityType.SIGN
                        || blockEntity.getType() == BlockEntityType.HANGING_SIGN
        );
    }

    @Override
    public boolean hasAnyText() {
        SignBlockEntity sign = (SignBlockEntity)(Object)this;
        SignText frontText = sign.getFrontText();
        SignText backText = sign.getBackText();

        if (frontText != cachedFrontText || backText != cachedBackText) {
            cachedFrontText = frontText;
            cachedBackText = backText;
            cachedHasAnyText = SpecialBlockEntityManager.hasAnyText(frontText, false)
                    || SpecialBlockEntityManager.hasAnyText(backText, false);
        }

        return cachedHasAnyText;
    }
}
