package betterblockentities.mixin.render.immediate.blockentity.lectern;

/* local */
import betterblockentities.client.render.immediate.blockentity.extentions.BlockEntityExt;
import betterblockentities.client.render.immediate.blockentity.extentions.LecternBlockEntityExt;
import betterblockentities.client.render.immediate.blockentity.manager.InstancedBlockEntityManager;

/* minecraft */
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/* mixin */
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LecternBlockEntity.class)
public class LecternBlockEntityMixin implements LecternBlockEntityExt {
    @Unique private BlockState cachedBookState;
    @Unique private boolean cachedHasBook;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void init(CallbackInfo ci) {
        BlockEntity blockEntity = (BlockEntity)(Object)this;
        BlockEntityExt ext = (BlockEntityExt)(Object)this;

        ext.terrainMeshReady(true);
        ext.hasSpecialManager(true);
        ext.optKind(InstancedBlockEntityManager.OptKind.LECTERN);
        ext.supportedBlockEntity(blockEntity.getType() == BlockEntityType.LECTERN);
    }

    @Override
    public boolean hasBookForRendering() {
        LecternBlockEntity lectern = (LecternBlockEntity)(Object)this;
        BlockState state = lectern.getBlockState();

        if (state != cachedBookState) {
            cachedBookState = state;
            cachedHasBook = state.getValue(LecternBlock.HAS_BOOK);
        }

        return cachedHasBook;
    }
}
