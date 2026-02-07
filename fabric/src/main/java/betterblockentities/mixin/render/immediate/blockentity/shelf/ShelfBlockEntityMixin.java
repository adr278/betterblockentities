package betterblockentities.mixin.render.immediate.blockentity.shelf;

/* local */
import betterblockentities.client.chunk.section.SectionUpdateDispatcher;
import betterblockentities.client.gui.config.ConfigCache;
import betterblockentities.client.render.immediate.blockentity.extentions.BlockEntityExt;
import betterblockentities.client.render.immediate.blockentity.manager.InstancedBlockEntityManager;
import betterblockentities.client.render.immediate.blockentity.manager.SpecialBlockEntityManager;

/* minecraft */
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
    @Inject(method = "<init>", at = @At("TAIL"))
    private void init(CallbackInfo ci) {
        BlockEntityExt ext = (BlockEntityExt)(Object)this;
        ext.supportedBlockEntity(true);
        ext.terrainMeshReady(true);
        ext.hasSpecialManager(SpecialBlockEntityManager.shelfUsesSpecialManager());
        ext.optKind(InstancedBlockEntityManager.OptKind.SHELF);
    }

    @Inject(method = "setChanged()V", at = @At("TAIL"))
    private void afterSetChanged(CallbackInfo ci)
    {dirtySectionIfOptimized();}

    @Inject(method = "loadAdditional(Lnet/minecraft/world/level/storage/ValueInput;)V",
            at = @At("TAIL"))
    private void afterLoadAdditional(ValueInput in, CallbackInfo ci)
    {dirtySectionIfOptimized();}

    @Unique private void dirtySectionIfOptimized() {
        ((BlockEntityExt)(Object)this).hasSpecialManager(SpecialBlockEntityManager.shelfUsesSpecialManager());
        if (!ConfigCache.optimizeShelves || !ConfigCache.optimizeShelfItems) return;

        ShelfBlockEntity self = (ShelfBlockEntity) (Object) this;
        Level level = self.getLevel();
        if (level == null || !level.isClientSide()) return;
        SectionUpdateDispatcher.queueRebuildAtBlockPos(self.getBlockPos());
    }
}
