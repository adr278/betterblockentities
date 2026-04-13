package betterblockentities.client.render.immediate.entity.renderers;

/* local */
import betterblockentities.client.chunk.pipeline.itemframe.ItemFrameContentRenderMode;
import betterblockentities.client.chunk.pipeline.itemframe.ItemFrameFallbackMapRenderStateCache;
import betterblockentities.client.chunk.pipeline.itemframe.ItemFrameMapSurfaceRegistry;
import betterblockentities.client.chunk.pipeline.itemframe.ItemFrameModelCapture;
import betterblockentities.client.chunk.pipeline.itemframe.ItemFrameRenderHelper;
import betterblockentities.client.chunk.pipeline.itemframe.MapPageCache;
import betterblockentities.client.chunk.pipeline.itemframe.MapLifecycleState;
import betterblockentities.client.gui.config.ConfigCache;
import betterblockentities.client.render.immediate.entity.ItemFrameRuntimeHelper;
import betterblockentities.client.render.immediate.entity.extensions.ItemFrameExt;
import betterblockentities.client.render.immediate.entity.state.BBEItemFrameRenderState;
import betterblockentities.mixin.render.immediate.entity.MapRendererAccessor;

/* minecraft */
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MapRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemFrameRenderer;
import net.minecraft.client.renderer.entity.state.ItemFrameRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.level.saveddata.maps.MapDecoration;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.phys.Vec3;

/* mojang */
import com.mojang.blaze3d.vertex.PoseStack;

/* annotations */
import org.jspecify.annotations.NonNull;

public class BBEItemFrameRenderer<T extends ItemFrame> extends ItemFrameRenderer<T> {
    private static final double VANILLA_ITEM_FRAME_IMMEDIATE_RANGE_BLOCKS = 10.0D * 16.0D;
    private static final double NAME_RENDER_DISTANCE_SQUARED = 4096.0D;
    private static final float MAP_ONLY_FALLBACK_Z_BIAS = -0.5F;

    private final MapRenderer mapRenderer;

    public BBEItemFrameRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.mapRenderer = context.getMapRenderer();
        ItemFrameModelCapture.setBlockModelResolver(context.getBlockModelResolver());
        MapPageCache.setDecorationSprites(((MapRendererAccessor) this.mapRenderer).getDecorationSprites());
    }

    @Override public @NonNull BBEItemFrameRenderState createRenderState() {
        return new BBEItemFrameRenderState();
    }

    @Override public boolean shouldRender(
            @NonNull T entity,
            @NonNull Frustum culler,
            double camX,
            double camY,
            double camZ
    ) {
        if (!optimizationEnabled()) return super.shouldRender(entity, culler, camX, camY, camZ);
        if (entity.isRemoved()) return false;

        ItemFrameExt ext = (ItemFrameExt) entity;

        double xd = entity.getX() - camX;
        double yd = entity.getY() - camY;
        double zd = entity.getZ() - camZ;
        double distanceSquared = xd * xd + yd * yd + zd * zd;
        boolean withinImmediateDistance = isWithinVanillaImmediateDistance(distanceSquared);

        if (!ItemFrameRuntimeHelper.wasUploadedStateRefreshedThisTick(ext)) ItemFrameRuntimeHelper.refreshUploadedState(entity);

        boolean terrainMapSurface = ext.terrainMeshActive() && ext.contentRenderMode() == ItemFrameContentRenderMode.SECTION_MAP_SURFACE;
        boolean submitMapLabels = terrainMapSurface && withinImmediateDistance && shouldSubmitMapLabels(entity, ext);

        if (submitMapLabels) return super.shouldRender(entity, culler, camX, camY, camZ);
        if (isTerrainOwnedWithoutImmediateSubmission(entity, ext, distanceSquared, submitMapLabels)) return false;
        if (!withinImmediateDistance) return false;

        return super.shouldRender(entity, culler, camX, camY, camZ);
    }

    @Override public void extractRenderState(
            @NonNull T entity,
            @NonNull ItemFrameRenderState baseState,
            float partialTicks
    ) {
        BBEItemFrameRenderState state = (BBEItemFrameRenderState) baseState;
        resetBBEState(state);
        state.entityType = entity.getType();

        if (!optimizationEnabled()) {
            super.extractRenderState(entity, baseState, partialTicks);
            state.renderImmediate = true;
            state.skipBodySubmission = false;
            state.renderImmediateContents = false;
            state.skipAllSubmission = false;
            state.contentRenderMode = ItemFrameContentRenderMode.NONE;
            return;
        }

        if (entity.isRemoved()) {
            state.renderImmediate = false;
            state.skipBodySubmission = true;
            state.renderImmediateContents = false;
            state.skipAllSubmission = true;
            state.contentRenderMode = ItemFrameContentRenderMode.NONE;
            return;
        }

        ItemFrameExt ext = (ItemFrameExt) entity;

        if (!ItemFrameRuntimeHelper.wasUploadedStateRefreshedThisTick(ext)) {
            ItemFrameRuntimeHelper.refreshUploadedState(entity);
        }

        double distanceSquared = distanceSquaredToCamera(entity);
        if (canUseMapLabelOnlyExtraction(entity, ext, distanceSquared)) {
            fillMapLabelOnlyState(entity, state, ext, distanceSquared, partialTicks);
            return;
        }
        if (canUseMapOnlyFallbackExtraction(entity, ext, distanceSquared)) {
            fillMapOnlyFallbackState(entity, state, ext, distanceSquared, partialTicks);
            return;
        }

        super.extractRenderState(entity, baseState, partialTicks);
        fillStateAfterVanillaExtraction(entity, state, ext);
    }

    @Override public void submit(
            @NonNull ItemFrameRenderState baseState,
            @NonNull PoseStack poseStack,
            @NonNull SubmitNodeCollector submitNodeCollector,
            @NonNull CameraRenderState camera
    ) {
        BBEItemFrameRenderState state = (BBEItemFrameRenderState) baseState;

        if (state.skipAllSubmission) return;
        if (state.renderImmediate || !state.skipBodySubmission) {
            super.submit(baseState, poseStack, submitNodeCollector, camera);
            return;
        }
        if (state.renderImmediateContents) {
            submitImmediateContents(baseState, poseStack, submitNodeCollector, camera);
            return;
        }

        super.submitNameDisplay(baseState, poseStack, submitNodeCollector, camera);
    }

    private static void resetBBEState(BBEItemFrameRenderState state) {
        state.renderImmediate = true;
        state.terrainMeshReady = false;
        state.skipBodySubmission = false;
        state.renderImmediateContents = false;
        state.skipAllSubmission = false;
        state.renderMapLabelsFromData = false;
        state.immediateMapLight = 0;
        state.contentRenderMode = ItemFrameContentRenderMode.NONE;
        state.mapRenderState.texture = null;
        state.mapRenderState.decorations.clear();
    }

    private void fillStateAfterVanillaExtraction(
            @NonNull T entity,
            @NonNull BBEItemFrameRenderState state,
            @NonNull ItemFrameExt ext
    ) {
        boolean withinImmediateDistance = isWithinVanillaImmediateDistance(state.distanceToCameraSq);

        state.terrainMeshReady = ext.terrainMeshReady();

        if (ext.latchedTerrainSnapshot() && !ext.terrainMeshActive()) {
            state.renderImmediate = withinImmediateDistance;
            state.skipBodySubmission = !withinImmediateDistance;
            state.renderImmediateContents = false;
            state.skipAllSubmission = !withinImmediateDistance;
            state.contentRenderMode = ext.latchedContentRenderMode();
            return;
        }

        if (!ext.terrainMeshReady()) {
            state.renderImmediate = withinImmediateDistance && ext.renderImmediateWhileWaiting();
            state.skipBodySubmission = !state.renderImmediate;
            state.renderImmediateContents = false;
            state.skipAllSubmission = !state.renderImmediate;
            state.contentRenderMode = ItemFrameContentRenderMode.NONE;
            return;
        }

        state.contentRenderMode = ext.contentRenderMode();

        if (ext.terrainMeshActive()) {
            boolean hasMapLabels = state.contentRenderMode == ItemFrameContentRenderMode.SECTION_MAP_SURFACE
                    && hasNamedDecorationLabels(state);

            state.renderImmediate = false;
            state.skipBodySubmission = true;
            state.renderImmediateContents = requiresImmediateContentSubmission(state.contentRenderMode, hasMapLabels)
                    && withinImmediateDistance;
            state.renderMapLabelsFromData = state.contentRenderMode == ItemFrameContentRenderMode.SECTION_MAP_SURFACE
                    && hasMapLabels;
            ItemFrameMapSurfaceRegistry.ActiveSurfaceState surfaceState = ItemFrameMapSurfaceRegistry.get(entity.getId());

            state.immediateMapLight = surfaceState != null ? surfaceState.mapLight() : state.lightCoords;

            state.skipAllSubmission = !state.renderImmediateContents && !entity.isCurrentlyGlowing() && !shouldSubmitName
                    (entity, state.distanceToCameraSq);
            return;
        }

        state.renderImmediate = withinImmediateDistance;
        state.skipBodySubmission = !withinImmediateDistance;
        state.renderImmediateContents = false;
        state.skipAllSubmission = !withinImmediateDistance;
        state.contentRenderMode = ext.contentRenderMode();
    }

    private boolean isTerrainOwnedWithoutImmediateSubmission(
            @NonNull T entity,
            @NonNull ItemFrameExt ext,
            double distanceSquared,
            boolean submitMapLabels
    ) {
        if (!ext.terrainMeshReady() || !ext.terrainMeshActive()) return false;

        return !terrainOwnedFrameRequiresImmediateSubmission(entity, ext, distanceSquared, submitMapLabels);
    }

    private boolean terrainOwnedFrameRequiresImmediateSubmission(
            @NonNull T entity,
            @NonNull ItemFrameExt ext,
            double distanceSquared,
            boolean submitMapLabels
    ) {
        if (requiresImmediateContentSubmissionFast(ext, distanceSquared, submitMapLabels)) return true;
        if (entity.isCurrentlyGlowing()) return true;

        return shouldSubmitName(entity, distanceSquared);
    }

    private boolean requiresImmediateContentSubmissionFast(
            @NonNull ItemFrameExt ext,
            double distanceSquared,
            boolean submitMapLabels
    ) {
        ItemFrameContentRenderMode contentRenderMode = ext.contentRenderMode();

        if (contentRenderMode == ItemFrameContentRenderMode.IMMEDIATE_ITEM) {
            return isWithinVanillaImmediateDistance(distanceSquared);
        }

        if (contentRenderMode == ItemFrameContentRenderMode.SECTION_MAP_SURFACE) {
            return submitMapLabels;
        }

        return false;
    }

    private boolean canUseMapLabelOnlyExtraction(
            @NonNull T entity,
            @NonNull ItemFrameExt ext,
            double distanceSquared
    ) {
        return ext.terrainMeshReady()
                && ext.terrainMeshActive()
                && ext.contentRenderMode() == ItemFrameContentRenderMode.SECTION_MAP_SURFACE
                && isWithinVanillaImmediateDistance(distanceSquared)
                && !entity.isCurrentlyGlowing()
                && !shouldSubmitName(entity, distanceSquared);
    }

    private boolean canUseMapOnlyFallbackExtraction(
            @NonNull T entity,
            @NonNull ItemFrameExt ext,
            double distanceSquared
    ) {
        MapLifecycleState lifecycleState = ext.mapLifecycleState();
        return ext.cachedFramedMapId() != null
                && !ext.terrainMeshActive()
                && ext.renderImmediateWhileWaiting()
                && (lifecycleState == MapLifecycleState.PENDING_DATA
                || lifecycleState == MapLifecycleState.NORMAL_FALLBACK)
                && isWithinVanillaImmediateDistance(distanceSquared)
                && !entity.isCurrentlyGlowing()
                && !shouldSubmitName(entity, distanceSquared);
    }

    private void fillMapLabelOnlyState(
            @NonNull T entity,
            @NonNull BBEItemFrameRenderState state,
            @NonNull ItemFrameExt ext,
            double distanceSquared,
            float partialTicks
    ) {
        state.entityType = entity.getType();
        state.x = Mth.lerp(partialTicks, entity.xOld, entity.getX());
        state.y = Mth.lerp(partialTicks, entity.yOld, entity.getY());
        state.z = Mth.lerp(partialTicks, entity.zOld, entity.getZ());
        state.ageInTicks = entity.tickCount + partialTicks;
        state.boundingBoxWidth = entity.getBbWidth();
        state.boundingBoxHeight = entity.getBbHeight();
        state.eyeHeight = entity.getEyeHeight();
        state.distanceToCameraSq = distanceSquared;
        state.isInvisible = entity.isInvisible();
        state.isDiscrete = entity.isDiscrete();
        state.displayFireAnimation = entity.displayFireAnimation();

        ItemFrameMapSurfaceRegistry.ActiveSurfaceState surfaceState =
                ItemFrameMapSurfaceRegistry.get(entity.getId());
        state.lightCoords = surfaceState != null
                ? surfaceState.mapLight()
                : this.getPackedLightCoords(entity, partialTicks);
        state.outlineColor = 0;
        state.passengerOffset = null;
        state.nameTag = null;
        state.nameTagAttachment = null;
        state.scoreText = null;
        state.leashStates = null;
        state.shadowRadius = 0.0F;
        state.shadowPieces.clear();

        state.direction = entity.getDirection();
        state.rotation = entity.getRotation();
        state.isGlowFrame = false;
        state.mapId = ext.cachedFramedMapId();
        state.frameModel.clear();
        state.item.clear();
        state.mapRenderState.decorations.clear();

        state.renderImmediate = false;
        state.terrainMeshReady = true;
        state.skipBodySubmission = true;
        state.renderImmediateContents = true;
        state.skipAllSubmission = false;
        state.renderMapLabelsFromData = true;
        state.immediateMapLight = state.lightCoords;
        state.contentRenderMode = ItemFrameContentRenderMode.SECTION_MAP_SURFACE;
    }

    private void fillMapOnlyFallbackState(
            @NonNull T entity,
            @NonNull BBEItemFrameRenderState state,
            @NonNull ItemFrameExt ext,
            double distanceSquared,
            float partialTicks
    ) {
        MapId mapId = ext.cachedFramedMapId();
        ClientLevel level = Minecraft.getInstance().level;
        MapItemSavedData mapData = level != null && mapId != null ? level.getMapData(mapId) : null;
        if (mapId == null || mapData == null) {
            state.renderImmediate = false;
            state.skipBodySubmission = true;
            state.renderImmediateContents = false;
            state.skipAllSubmission = true;
            state.contentRenderMode = ItemFrameContentRenderMode.NONE;
            return;
        }

        state.entityType = entity.getType();
        state.x = Mth.lerp(partialTicks, entity.xOld, entity.getX());
        state.y = Mth.lerp(partialTicks, entity.yOld, entity.getY());
        state.z = Mth.lerp(partialTicks, entity.zOld, entity.getZ());
        state.ageInTicks = entity.tickCount + partialTicks;
        state.boundingBoxWidth = entity.getBbWidth();
        state.boundingBoxHeight = entity.getBbHeight();
        state.eyeHeight = entity.getEyeHeight();
        state.distanceToCameraSq = distanceSquared;
        state.isInvisible = entity.isInvisible();
        state.isDiscrete = entity.isDiscrete();
        state.displayFireAnimation = entity.displayFireAnimation();
        state.lightCoords = fallbackMapLightCoords(entity, ext, partialTicks);
        state.outlineColor = 0;
        state.passengerOffset = null;
        state.nameTag = null;
        state.nameTagAttachment = null;
        state.scoreText = null;
        state.leashStates = null;
        state.shadowRadius = 0.0F;
        state.shadowPieces.clear();

        state.direction = entity.getDirection();
        state.rotation = entity.getRotation();
        state.isGlowFrame = entity.is(EntityType.GLOW_ITEM_FRAME);
        state.mapId = mapId;
        state.frameModel.clear();
        state.item.clear();
        ItemFrameFallbackMapRenderStateCache.extract(this.mapRenderer, mapId, mapData, state.mapRenderState);

        state.renderImmediate = false;
        state.terrainMeshReady = ext.terrainMeshReady();
        state.skipBodySubmission = true;
        state.renderImmediateContents = true;
        state.skipAllSubmission = false;
        state.renderMapLabelsFromData = false;
        state.immediateMapLight = state.lightCoords;
        state.contentRenderMode = ItemFrameContentRenderMode.NONE;
    }

    private boolean shouldSubmitName(@NonNull T entity, double distanceSquared) {
        return isHoveringFrame(entity, distanceSquared)
                && entity.getItem().getCustomName() != null;
    }

    private int fallbackMapLightCoords(
            @NonNull T entity,
            @NonNull ItemFrameExt ext,
            float partialTicks
    ) {
        long gameTime = entity.level().getGameTime();
        BlockPos framePos = entity.getPos();
        Direction supportDirection = entity.getDirection().getOpposite();
        int supportX = framePos.getX() + supportDirection.getStepX();
        int supportY = framePos.getY() + supportDirection.getStepY();
        int supportZ = framePos.getZ() + supportDirection.getStepZ();
        BlockPos cachedSupportPos = ext.fallbackMapLightSupportPos();

        if (ext.fallbackMapLightGameTime() == gameTime
                && cachedSupportPos != null
                && cachedSupportPos.getX() == supportX
                && cachedSupportPos.getY() == supportY
                && cachedSupportPos.getZ() == supportZ) {
            return ext.fallbackMapLight();
        }

        int light = this.getPackedLightCoords(entity, partialTicks);
        ext.fallbackMapLight(light);
        ext.fallbackMapLightGameTime(gameTime);
        ext.fallbackMapLightSupportPos(new BlockPos(supportX, supportY, supportZ));
        return light;
    }

    private boolean shouldSubmitMapLabels(@NonNull T entity, @NonNull ItemFrameExt ext) {
        return ItemFrameRuntimeHelper.hasNamedDecorationLabels(
                entity.level(),
                ext.cachedFramedMapId()
        );
    }

    private boolean isHoveringFrame(@NonNull T entity, double distanceSquared) {
        return distanceSquared < NAME_RENDER_DISTANCE_SQUARED
                && Minecraft.renderNames()
                && entity == this.entityRenderDispatcher.crosshairPickEntity;
    }

    private static double distanceSquaredToCamera(@NonNull ItemFrame entity) {
        Vec3 cameraPos = Minecraft.getInstance()
                .gameRenderer
                .getGameRenderState()
                .levelRenderState
                .cameraRenderState
                .pos;

        double dx = entity.getX() - cameraPos.x();
        double dy = entity.getY() - cameraPos.y();
        double dz = entity.getZ() - cameraPos.z();

        return dx * dx + dy * dy + dz * dz;
    }

    private void submitImmediateContents(
            @NonNull ItemFrameRenderState state,
            @NonNull PoseStack poseStack,
            @NonNull SubmitNodeCollector submitNodeCollector,
            @NonNull CameraRenderState camera
    ) {
        if (state.nameTag != null) super.submitNameDisplay(state, poseStack, submitNodeCollector, camera);

        poseStack.pushPose();

        Vec3 renderOffset = ItemFrameRenderHelper.getRenderOffset(state.direction);
        poseStack.translate(-renderOffset.x(), -renderOffset.y(), -renderOffset.z());

        MapId mapId = state.mapId;
        if (mapId != null) {
            BBEItemFrameRenderState bbeState = (BBEItemFrameRenderState) state;
            boolean mapOnlyFallback = bbeState.contentRenderMode == ItemFrameContentRenderMode.NONE
                    && !bbeState.renderMapLabelsFromData;
            int light = bbeState.renderMapLabelsFromData
                    ? bbeState.immediateMapLight
                    : ItemFrameRenderHelper.getMapLight(state.isGlowFrame, state.lightCoords);

            ItemFrameRenderHelper.applyFacingPose(poseStack, state.direction);
            ItemFrameRenderHelper.translateToContentPlane(poseStack, state.isInvisible);
            ItemFrameRenderHelper.applyMapPose(poseStack, state.rotation);
            if (mapOnlyFallback) poseStack.translate(0.0F, 0.0F, MAP_ONLY_FALLBACK_Z_BIAS);

            if (bbeState.renderMapLabelsFromData) {
                submitImmediateMapLabelsFromData(state, poseStack, submitNodeCollector, light);
            } else if (bbeState.contentRenderMode == ItemFrameContentRenderMode.SECTION_MAP_SURFACE) {
                submitImmediateMapLabelsOnly(state, poseStack, submitNodeCollector, light);
            } else {
                this.mapRenderer.render(state.mapRenderState, poseStack, submitNodeCollector, true, light);
            }
        } else if (!state.item.isEmpty()) {
            ItemFrameRenderHelper.applyFacingPose(poseStack, state.direction);
            ItemFrameRenderHelper.translateToContentPlane(poseStack, state.isInvisible);
            ItemFrameRenderHelper.applyItemPose(poseStack, state.rotation);

            int light = ItemFrameRenderHelper.getItemLight(state.isGlowFrame, state.lightCoords);
            state.item.submit(poseStack, submitNodeCollector, light, OverlayTexture.NO_OVERLAY, state.outlineColor);
        }

        poseStack.popPose();
    }

    private static boolean hasNamedDecorationLabels(@NonNull ItemFrameRenderState state) {
        if (state.mapId == null) return false;

        for (var decoration : state.mapRenderState.decorations) {
            if (decoration.renderOnFrame && decoration.name != null) return true;
        }

        return false;
    }

    private static void submitImmediateMapLabelsOnly(
            @NonNull ItemFrameRenderState state,
            @NonNull PoseStack poseStack,
            @NonNull SubmitNodeCollector submitNodeCollector,
            int light
    ) {
        for (var decoration : state.mapRenderState.decorations) {
            if (!decoration.renderOnFrame || decoration.name == null) continue;

            submitImmediateMapLabel(
                    decoration.name,
                    decoration.x,
                    decoration.y,
                    poseStack,
                    submitNodeCollector,
                    light
            );
        }
    }

    private static void submitImmediateMapLabelsFromData(
            @NonNull ItemFrameRenderState state,
            @NonNull PoseStack poseStack,
            @NonNull SubmitNodeCollector submitNodeCollector,
            int light
    ) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || state.mapId == null) return;

        MapItemSavedData mapData = level.getMapData(state.mapId);
        if (mapData == null) return;

        for (MapDecoration decoration : mapData.getDecorations()) {
            if (!decoration.renderOnFrame()) continue;

            Component name = decoration.name().orElse(null);
            if (name == null) continue;

            submitImmediateMapLabel(
                    name,
                    decoration.x(),
                    decoration.y(),
                    poseStack,
                    submitNodeCollector,
                    light
            );
        }
    }

    private static void submitImmediateMapLabel(
            @NonNull Component name,
            float x,
            float y,
            @NonNull PoseStack poseStack,
            @NonNull SubmitNodeCollector submitNodeCollector,
            int light
    ) {
        Font font = Minecraft.getInstance().font;
        float width = font.width(name);
        float scale = Mth.clamp(25.0F / width, 0.0F, 6.0F / 9.0F);

        poseStack.pushPose();
        poseStack.translate(
                x / 2.0F + 64.0F - width * scale / 2.0F,
                y / 2.0F + 64.0F + 4.0F,
                -0.025F
        );
        poseStack.scale(scale, scale, -1.0F);
        poseStack.translate(0.0F, 0.0F, 0.1F);

        submitNodeCollector.order(1)
                .submitText(
                        poseStack,
                        0.0F,
                        0.0F,
                        name.getVisualOrderText(),
                        false,
                        Font.DisplayMode.NORMAL,
                        light,
                        -1,
                        Integer.MIN_VALUE,
                        0
                );

        poseStack.popPose();
    }

    private static boolean requiresImmediateContentSubmission(
            @NonNull ItemFrameContentRenderMode contentRenderMode,
            boolean hasNamedDecorationLabels
    ) {
        return contentRenderMode == ItemFrameContentRenderMode.IMMEDIATE_ITEM
                || (contentRenderMode == ItemFrameContentRenderMode.SECTION_MAP_SURFACE
                && hasNamedDecorationLabels);
    }

    private static boolean isWithinVanillaImmediateDistance(double distanceSquared) {
        return distanceSquared < VANILLA_ITEM_FRAME_IMMEDIATE_RANGE_BLOCKS * VANILLA_ITEM_FRAME_IMMEDIATE_RANGE_BLOCKS;
    }

    private static boolean optimizationEnabled() {
        return ConfigCache.masterOptimize && ConfigCache.optimizeItemFrames;
    }
}
