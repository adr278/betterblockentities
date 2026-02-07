package betterblockentities.client.render.immediate.blockentity.renderers;

/* local */
import betterblockentities.client.chunk.pipeline.shelf.ShelfItemImmediateFallback;
import betterblockentities.client.gui.config.ConfigCache;

/* minecraft */
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.ShelfRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.ShelfBlock;
import net.minecraft.world.level.block.entity.ShelfBlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/* mojang */
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

/* fastutil */
import it.unimi.dsi.fastutil.HashCommon;

/* annotations */
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;

public class BBEImmediateShelfItemRenderer implements BlockEntityRenderer<ShelfBlockEntity, ShelfRenderState> {
    private static final float ALIGN_ITEMS_TO_BOTTOM = -0.25F;
    private final ItemModelResolver itemModelResolver;

    public BBEImmediateShelfItemRenderer(final BlockEntityRendererProvider.Context context) {
        this.itemModelResolver = context.itemModelResolver();
    }

    @Override public @NonNull ShelfRenderState createRenderState() {
        return new ShelfRenderState();
    }

    @Override public void extractRenderState(
            final @NonNull ShelfBlockEntity blockEntity,
            final @NonNull ShelfRenderState state,
            final float partialTicks,
            final @NonNull Vec3 cameraPosition,
            final ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress
    ) {
        assert breakProgress != null;
        BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress);

        Arrays.fill(state.items, null);

        if (!ConfigCache.optimizeShelves) return;

        state.alignToBottom = blockEntity.getAlignItemsToBottom();
        state.facing = blockEntity.getBlockState().getValue(ShelfBlock.FACING);

        NonNullList<ItemStack> items = blockEntity.getItems();
        int seed = HashCommon.long2int(blockEntity.getBlockPos().asLong());

        for (int slot = 0; slot < state.items.length; slot++) {

            ItemStack itemStack = items.get(slot);
            if (!shouldRenderInImmediate(blockEntity, itemStack, slot)) continue;

            ItemStackRenderState itemStackRenderState = new ItemStackRenderState();
            this.itemModelResolver.updateForTopItem(
                    itemStackRenderState,
                    itemStack,
                    ItemDisplayContext.ON_SHELF,
                    blockEntity.level(),
                    blockEntity,
                    seed + slot
            );

            if (!itemStackRenderState.isEmpty()) state.items[slot] = itemStackRenderState;
        }
    }

    private static boolean shouldRenderInImmediate(
            ShelfBlockEntity blockEntity,
            ItemStack itemStack,
            int slot
    ) {
        return ShelfItemImmediateFallback.shouldUseImmediateFallback(blockEntity, itemStack, slot);
    }

    @Override public void submit(
            final ShelfRenderState state,
            final @NonNull PoseStack poseStack,
            final @NonNull SubmitNodeCollector submitNodeCollector,
            final @NonNull CameraRenderState camera
    ) {
        float yRot = state.facing.getAxis().isHorizontal() ? -state.facing.toYRot() : 180.0F;

        for (int slot = 0; slot < state.items.length; slot++) {
            ItemStackRenderState itemStackRenderState = state.items[slot];
            if (itemStackRenderState != null)
                this.submitItem(state, itemStackRenderState, poseStack, submitNodeCollector, slot, yRot);
        }
    }

    private void submitItem(
            final ShelfRenderState state,
            final ItemStackRenderState itemStackRenderState,
            final PoseStack poseStack,
            final SubmitNodeCollector submitNodeCollector,
            final int slot,
            final float yRot
    ) {
        float itemSlotPosition = (slot - 1) * 0.3125F;
        Vec3 itemOffset = new Vec3(itemSlotPosition, state.alignToBottom ? ALIGN_ITEMS_TO_BOTTOM : 0.0, -0.25);

        poseStack.pushPose();
        poseStack.translate(0.5F, 0.5F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(yRot));
        poseStack.translate(itemOffset);
        poseStack.scale(0.25F, 0.25F, 0.25F);

        AABB box = itemStackRenderState.getModelBoundingBox();
        double offsetY = -box.minY;
        if (!state.alignToBottom) offsetY += -(box.maxY - box.minY) / 2.0;

        poseStack.translate(0.0, offsetY, 0.0);
        itemStackRenderState.submit(poseStack, submitNodeCollector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        poseStack.popPose();
    }
}
