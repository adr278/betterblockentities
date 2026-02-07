package betterblockentities.mixin.render.immediate.blockentity.shelf;

/* minecraft */
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.ShelfBlockEntity;
import net.minecraft.world.level.storage.ValueInput;

/* mixin */
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ShelfBlockEntity.class)
public class ShelfBlockEntityMixin {

    @Inject(method = "loadAdditional", at = @At("TAIL"))
    private void afterLoadAdditional(ValueInput in, CallbackInfo ci) {
        dirtySection();
    }

    @Inject(method = "applyImplicitComponents", at = @At("TAIL"))
    private void afterApplyImplicitComponents(DataComponentGetter getter, CallbackInfo ci) {
        dirtySection();
    }

    @Inject(method = "setChanged()V", at = @At("TAIL"))
    private void afterSetChanged(CallbackInfo ci) {
        dirtySection();
    }

    @Unique @SuppressWarnings("ConstantConditions") private void dirtySection() {
        Level level = ((ShelfBlockEntity)(Object)this).getLevel();
        if (level == null || !level.isClientSide()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.levelRenderer == null) return;

        BlockPos p = ((ShelfBlockEntity)(Object)this).getBlockPos();
        mc.levelRenderer.setBlocksDirty(p.getX(), p.getY(), p.getZ(), p.getX(), p.getY(), p.getZ());
    }
}
