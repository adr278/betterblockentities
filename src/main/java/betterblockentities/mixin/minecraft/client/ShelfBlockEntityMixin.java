package betterblockentities.mixin.minecraft.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.ShelfBlockEntity;
import net.minecraft.world.level.storage.ValueInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ShelfBlockEntity.class)
public class ShelfBlockEntityMixin {

    // When BE NBT syncs in (initial chunk load or update packets), force rebuild
    @Inject(method = "loadAdditional", at = @At("TAIL"))
    private void afterLoadAdditional(ValueInput in, CallbackInfo ci) {
        dirtySection();
    }

    // When implicit components apply (container component path), force rebuild
    @Inject(method = "applyImplicitComponents", at = @At("TAIL"))
    private void afterApplyImplicitComponents(DataComponentGetter getter, CallbackInfo ci) {
        dirtySection();
    }

    // When a player interaction changes the shelf, and it calls setChanged(), force rebuild
    @Inject(method = "setChanged()V", at = @At("TAIL"))
    private void afterSetChanged(CallbackInfo ci) {
        dirtySection();
    }

    @Unique private void dirtySection() {
        ShelfBlockEntity self = (ShelfBlockEntity) (Object) this;
        Level level = self.getLevel();
        if (level == null || !level.isClientSide()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.levelRenderer == null) return;

        BlockPos p = self.getBlockPos();

        // Forces the renderer/Sodium to rebuild the section mesh containing this block
        mc.levelRenderer.setBlocksDirty(p.getX(), p.getY(), p.getZ(), p.getX(), p.getY(), p.getZ());
    }
}
