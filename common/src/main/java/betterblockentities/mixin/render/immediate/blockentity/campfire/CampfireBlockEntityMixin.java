package betterblockentities.mixin.render.immediate.blockentity.campfire;

/* local */
import betterblockentities.client.render.immediate.blockentity.extentions.BlockEntityExt;
import betterblockentities.client.render.immediate.blockentity.extentions.CampfireBlockEntityExt;
import betterblockentities.client.render.immediate.blockentity.manager.InstancedBlockEntityManager;

/* minecraft */
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/* mixin */
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CampfireBlockEntity.class)
public class CampfireBlockEntityMixin implements CampfireBlockEntityExt {
    @Shadow @Final private NonNullList<ItemStack> items;

    @Unique private boolean hasRenderableItems;
    @Unique private boolean renderableItemsDirty;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void init(CallbackInfo ci) {
        BlockEntity blockEntity = (BlockEntity)(Object)this;
        BlockEntityExt ext = (BlockEntityExt)(Object)blockEntity;

        ext.terrainMeshReady(true);
        ext.hasSpecialManager(true);
        ext.optKind(InstancedBlockEntityManager.OptKind.CAMPFIRE);

        ext.supportedBlockEntity(blockEntity.getType() == BlockEntityType.CAMPFIRE);
        refreshRenderableItems();
    }

    @Inject(method = "loadAdditional", at = @At("TAIL"))
    private void refreshAfterLoad(CompoundTag tag, HolderLookup.Provider registries, CallbackInfo ci) {
        refreshRenderableItems();
    }

    @Inject(method = "applyImplicitComponents", at = @At("TAIL"))
    private void refreshAfterApplyingComponents(CallbackInfo ci) {
        refreshRenderableItems();
    }

    @Inject(method = "getItems", at = @At("HEAD"))
    private void markRenderableItemsDirty(CallbackInfoReturnable<NonNullList<ItemStack>> cir) {
        renderableItemsDirty = true;
    }

    @Inject(method = "markUpdated", at = @At("HEAD"))
    private void refreshAfterMutation(CallbackInfo ci) {
        refreshRenderableItems();
    }

    @Inject(
            method = "cookTick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/core/NonNullList;set(ILjava/lang/Object;)Ljava/lang/Object;",
                    shift = At.Shift.AFTER
            )
    )
    private static void refreshAfterCooking(
            Level level,
            BlockPos pos,
            BlockState state,
            CampfireBlockEntity campfire,
            CallbackInfo ci
    ) {
        ((CampfireBlockEntityExt)campfire).refreshRenderableItems();
    }

    @Inject(method = "clearContent", at = @At("TAIL"))
    private void refreshAfterClear(CallbackInfo ci) {
        refreshRenderableItems();
    }

    @Override
    public boolean hasRenderableItems() {
        if (renderableItemsDirty) {
            refreshRenderableItems();
        }

        return hasRenderableItems;
    }

    @Override
    public void refreshRenderableItems() {
        renderableItemsDirty = false;

        for (int i = 0, size = items.size(); i < size; i++) {
            if (!items.get(i).isEmpty()) {
                hasRenderableItems = true;
                return;
            }
        }

        hasRenderableItems = false;
    }
}
