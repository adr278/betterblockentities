package betterblockentities.client.render.immediate.entity.renderers;

/* local */
import betterblockentities.client.chunk.pipeline.itemframe.ItemFrameContentRenderMode;
import betterblockentities.client.gui.config.ConfigCache;
import betterblockentities.client.render.immediate.entity.ItemFrameRuntimeHelper;
import betterblockentities.client.render.immediate.entity.extensions.ItemFrameExt;

/* minecraft */
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemFrameRenderer;
import net.minecraft.client.renderer.entity.state.ItemFrameRenderState;
import net.minecraft.world.entity.decoration.ItemFrame;

/* annotations */
import org.jspecify.annotations.NonNull;

public class BBEItemFrameRenderer<T extends ItemFrame> extends ItemFrameRenderer<T> {
    private static final double VANILLA_ITEM_FRAME_IMMEDIATE_RANGE_BLOCKS = 10.0D * 16.0D;
    private static final double NAME_RENDER_DISTANCE_SQUARED = 4096.0D;

    public BBEItemFrameRenderer(EntityRendererProvider.Context context) { super(context); }

    @Override public boolean shouldRender(
            @NonNull T entity,
            @NonNull Frustum culler,
            double camX,
            double camY,
            double camZ
    ) {
        if (optimizationEnabled()) return super.shouldRender(entity, culler, camX, camY, camZ);
        if (entity.isRemoved()) return false;

        ItemFrameExt ext = (ItemFrameExt) entity;

        double xd = entity.getX() - camX;
        double yd = entity.getY() - camY;
        double zd = entity.getZ() - camZ;
        double distanceSquared = xd * xd + yd * yd + zd * zd;
        if (!isWithinVanillaImmediateDistance(distanceSquared)) return false;

        refreshTerrainStateIfNeeded(entity, ext);

        if (isTerrainOwnedWithoutImmediateSubmission(entity, ext, distanceSquared)) return false;
        if (!allowsImmediateSubmission(ext)) return false;

        return super.shouldRender(entity, culler, camX, camY, camZ);
    }

    @Override public void extractRenderState(
            @NonNull T entity,
            @NonNull ItemFrameRenderState baseState,
            float partialTicks
    ) {
        baseState.entityType = entity.getType();

        if (optimizationEnabled()) {
            super.extractRenderState(entity, baseState, partialTicks);
            return;
        }

        if (entity.isRemoved()) {
            clearVanillaGeometry(baseState);
            return;
        }

        ItemFrameExt ext = (ItemFrameExt) entity;

        super.extractRenderState(entity, baseState, partialTicks);
        suppressTerrainOwnedVanillaGeometry(baseState, ext);
    }

    private void suppressTerrainOwnedVanillaGeometry(
            @NonNull ItemFrameRenderState state,
            @NonNull ItemFrameExt ext
    ) {
        if (!ext.terrainMeshReady() || !ext.terrainMeshActive()) return;

        state.frameModel.clear();
        state.mapId = null;

        boolean submitItemImmediately = ext.contentRenderMode() == ItemFrameContentRenderMode.IMMEDIATE_ITEM
                && isWithinVanillaImmediateDistance(state.distanceToCameraSq);
        if (!submitItemImmediately) state.item.clear();
    }

    private boolean isTerrainOwnedWithoutImmediateSubmission(
            @NonNull T entity,
            @NonNull ItemFrameExt ext,
            double distanceSquared
    ) {
        if (!ext.terrainMeshReady() || !ext.terrainMeshActive()) return false;

        if (ext.contentRenderMode() == ItemFrameContentRenderMode.IMMEDIATE_ITEM) {
            return false;
        }
        if (entity.isCurrentlyGlowing()) return false;

        return !shouldSubmitName(entity, distanceSquared);
    }

    private static boolean allowsImmediateSubmission(@NonNull ItemFrameExt ext) {
        if (!ext.terrainMeshReady()) return false;
        if (ext.terrainMeshActive()) return true;
        return ext.contentRenderMode() == ItemFrameContentRenderMode.IMMEDIATE_ITEM;
    }

    private static void refreshTerrainStateIfNeeded(@NonNull ItemFrame entity, @NonNull ItemFrameExt ext) {
        if (needsTerrainStateRefresh(ext)) ItemFrameRuntimeHelper.refreshTerrainState(entity);
    }

    private static boolean needsTerrainStateRefresh(@NonNull ItemFrameExt ext) {
        if (!ext.terrainMeshReady()) return true;
        return !ext.terrainMeshActive() && ext.contentRenderMode() != ItemFrameContentRenderMode.IMMEDIATE_ITEM;
    }

    private boolean shouldSubmitName(@NonNull T entity, double distanceSquared) {
        return isHoveringFrame(entity, distanceSquared) && entity.getItem().getCustomName() != null;
    }

    private boolean isHoveringFrame(@NonNull T entity, double distanceSquared) {
        return distanceSquared < NAME_RENDER_DISTANCE_SQUARED
                && Minecraft.renderNames()
                && entity == this.entityRenderDispatcher.crosshairPickEntity;
    }

    private static void clearVanillaGeometry(@NonNull ItemFrameRenderState state) {
        state.frameModel.clear();
        state.item.clear();
        state.mapId = null;
    }

    private static boolean isWithinVanillaImmediateDistance(double distanceSquared) {
        return distanceSquared < VANILLA_ITEM_FRAME_IMMEDIATE_RANGE_BLOCKS * VANILLA_ITEM_FRAME_IMMEDIATE_RANGE_BLOCKS;
    }

    private static boolean optimizationEnabled() {
        return !ConfigCache.masterOptimize || !ConfigCache.optimizeItemFrames;
    }
}
