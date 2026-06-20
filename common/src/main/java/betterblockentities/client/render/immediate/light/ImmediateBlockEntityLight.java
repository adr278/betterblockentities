package betterblockentities.client.render.immediate.light;

/* local */
import betterblockentities.client.compat.IrisCompat;
import betterblockentities.client.gui.config.BBEConfig;
import betterblockentities.client.gui.config.ConfigCache;
import betterblockentities.client.render.immediate.blockentity.extentions.BlockEntityExt;
import betterblockentities.client.render.immediate.blockentity.extentions.BlockEntityRenderStateExt;
import betterblockentities.client.render.immediate.blockentity.manager.InstancedBlockEntityManager;
import betterblockentities.client.render.immediate.blockentity.misc.RenderingMode;

/* mojang */
import com.mojang.blaze3d.vertex.PoseStack;

/* minecraft */
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.blockentity.ChestRenderer;
import net.minecraft.client.renderer.blockentity.DecoratedPotRenderer;
import net.minecraft.client.renderer.blockentity.ShulkerBoxRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/* sodium */
import net.caffeinemc.mods.sodium.client.model.light.LightMode;
import net.caffeinemc.mods.sodium.client.model.light.LightPipelineProvider;
import net.caffeinemc.mods.sodium.client.model.light.data.QuadLightData;
import net.caffeinemc.mods.sodium.client.model.light.data.SingleBlockLightDataCache;
import net.caffeinemc.mods.sodium.client.render.model.EncodingFormat;
import net.caffeinemc.mods.sodium.client.render.model.MutableQuadViewImpl;
import net.caffeinemc.mods.sodium.client.render.model.SodiumShadeMode;
import net.caffeinemc.mods.sodium.client.services.PlatformBlockAccess;

/* java/misc */
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import java.util.ArrayDeque;

public final class ImmediateBlockEntityLight {
    private static final ThreadLocal<Matrix4f> PENDING_LIGHT_POSE = new ThreadLocal<>();
    private static final ThreadLocal<RenderContext> ACTIVE = new ThreadLocal<>();

    private static volatile Thread activeThread;
    private static RenderContext activeContext;

    public static Parameters createParameters(BlockEntityRenderState state) {
        if (IrisCompat.isShaderPackInUse()) {
            return null;
        }

        BlockEntity blockEntity = ((BlockEntityRenderStateExt)state).bbe$getBlockEntity();
        if (blockEntity == null) {
            return null;
        }

        BlockEntityExt ext = (BlockEntityExt)blockEntity;
        byte optKind = ext.bbe$getOptKind();
        boolean usesImmediateRenderer = ext.bbe$getRenderingMode() == RenderingMode.IMMEDIATE
                || !ext.bbe$isTerrainMeshReady();
        if (!ext.bbe$isSupportedBlockEntity()
                || !usesImmediateRenderer
                || !BBEConfig.OptEnabledTable.ENABLED[optKind & 0xFF]
                || !ConfigCache.isImmediateLightingEnabled(optKind)) {
            return null;
        }

        Level level = blockEntity.getLevel();
        if (!(level instanceof BlockAndTintGetter blockAndTintGetter)) {
            return null;
        }

        BlockState blockState = blockEntity.getBlockState();

        return new Parameters(
                blockAndTintGetter,
                blockState,
                state.blockPos,
                SodiumShadeMode.ENHANCED,
                null,
                null,
                optKind,
                ConfigCache.isMovingLightingEnabled(optKind)
        );
    }

    public static Matrix4f pendingLightPose() {
        return PENDING_LIGHT_POSE.get();
    }

    public static Scope pushActive(Parameters parameters) {
        Thread thread = Thread.currentThread();
        RenderContext previous = ACTIVE.get();
        RenderContext next = parameters == null ? null : new RenderContext(parameters);
        setActive(thread, next);
        return () -> restore(thread, previous);
    }

    public static RenderContext active() {
        if (activeThread == Thread.currentThread()) {
            return activeContext;
        }
        return ACTIVE.get();
    }

    public static <S> void submitModel(
            OrderedSubmitNodeCollector submitNodeCollector,
            Parameters parameters,
            Model<? super S> model,
            S state,
            PoseStack poseStack,
            RenderType renderType,
            int lightCoords,
            int overlayCoords,
            int tintedColor,
            TextureAtlasSprite sprite,
            int outlineColor,
            ModelFeatureRenderer.CrumblingOverlay crumblingOverlay
    ) {
        Parameters resolvedParameters = resolveParameters(parameters, poseStack);
        if (submitNodeCollector instanceof SubmitNodeCollection submitNodeCollection) {
            submitModel(
                    submitNodeCollection,
                    resolvedParameters,
                    model,
                    state,
                    poseStack,
                    renderType,
                    lightCoords,
                    overlayCoords,
                    tintedColor,
                    sprite,
                    outlineColor,
                    crumblingOverlay
            );
            return;
        }

        submitNodeCollector.submitModel(
                model,
                state,
                poseStack,
                renderType,
                lightCoords,
                overlayCoords,
                tintedColor,
                sprite,
                outlineColor,
                crumblingOverlay
        );
    }

    private static Parameters resolveParameters(Parameters parameters, PoseStack poseStack) {
        if (parameters == null) {
            return null;
        }

        if (parameters.movingLighting()) {
            return parameters.withLightPose(toLocalLightPose(parameters.rootPose(), poseStack.last().pose()));
        }

        Matrix4fc pendingPose = pendingLightPose();
        if (pendingPose != null) {
            return parameters.withLightPose(toLocalLightPose(parameters.rootPose(), pendingPose));
        }

        return parameters.withLightPose(fixedLightPose(parameters));
    }

    private static Matrix4fc fixedLightPose(Parameters parameters) {
        BlockState blockState = parameters.blockState();
        return switch (parameters.optKind()) {
            case InstancedBlockEntityManager.OptKind.CHEST -> ChestRenderer.modelTransformation(
                    blockState.getValueOrElse(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH)
            ).getMatrix();
            case InstancedBlockEntityManager.OptKind.SHULKER -> ShulkerBoxRenderer.modelTransform(
                    blockState.getValueOrElse(BlockStateProperties.FACING, Direction.UP)
            ).getMatrix();
            case InstancedBlockEntityManager.OptKind.POT -> DecoratedPotRenderer.modelTransformation(
                    blockState.getValueOrElse(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH)
            ).getMatrix();
            default -> new Matrix4f();
        };
    }

    private static Matrix4f toLocalLightPose(Matrix4fc rootPose, Matrix4fc lightPose) {
        if (lightPose == null) {
            return null;
        }

        if (rootPose == null) {
            return new Matrix4f(lightPose);
        }

        Matrix4f localPose = new Matrix4f(rootPose).invert();
        localPose.mul(lightPose);
        return localPose;
    }

    private static void setActive(Thread thread, RenderContext context) {
        if (context == null) {
            ACTIVE.remove();
        } else {
            ACTIVE.set(context);
        }

        if (activeThread == null || activeThread == thread) {
            activeContext = context;
            // Cache the null state too: ordinary entity models are the most frequent callers.
            activeThread = thread;
        }
    }

    private static void restore(Thread thread, RenderContext previous) {
        if (previous == null) {
            ACTIVE.remove();
        } else {
            ACTIVE.set(previous);
        }

        if (activeThread == thread) {
            activeContext = previous;
            // Keep the render thread's cached null state between model submissions. PoseStack
            // hooks run throughout the frame and otherwise fall back to ThreadLocal#get.
            activeThread = thread;
        }
    }

    private static <S> void submitModel(
            SubmitNodeCollection submitNodeCollection,
            Parameters parameters,
            Model<? super S> model,
            S state,
            PoseStack poseStack,
            RenderType renderType,
            int lightCoords,
            int overlayCoords,
            int tintedColor,
            TextureAtlasSprite sprite,
            int outlineColor,
            ModelFeatureRenderer.CrumblingOverlay crumblingOverlay
    ) {
        PoseStack.Pose pose = poseStack.last().copy();

        if (!renderType.isOutline()) {
            ModelFeatureRenderer.Submit<S> submit = submit(
                    parameters,
                    renderType,
                    pose,
                    model,
                    state,
                    lightCoords,
                    overlayCoords,
                    tintedColor,
                    sprite,
                    null
            );

            if (renderType == RenderTypes.waterMask()) {
                submitNodeCollection.waterMask.submit(submit);
            } else if (renderType.hasBlending()) {
                submitNodeCollection.translucentModels.submit(submit);
            } else {
                submitNodeCollection.solid.submit(submit);
            }
        }

        if (outlineColor != 0) {
            RenderType outlineRenderType = outlineRenderType(renderType);
            if (outlineRenderType != null) {
                submitNodeCollection.outline.submit(submit(
                        null,
                        outlineRenderType,
                        pose,
                        model,
                        state,
                        LightCoordsUtil.FULL_BRIGHT,
                        OverlayTexture.NO_OVERLAY,
                        outlineColor,
                        sprite,
                        null
                ));
            }
        }

        if (crumblingOverlay != null && renderType.affectsCrumbling()) {
            RenderType crumblingRenderType = ModelBakery.DESTROY_TYPES.get(crumblingOverlay.progress());
            submitNodeCollection.breakingOverlay.submit(submit(
                    parameters,
                    crumblingRenderType,
                    pose,
                    model,
                    state,
                    lightCoords,
                    overlayCoords,
                    tintedColor,
                    null,
                    crumblingOverlay.cameraPose()
            ));
        }
    }

    private static <S> ModelFeatureRenderer.Submit<S> submit(
            Parameters parameters,
            RenderType renderType,
            PoseStack.Pose pose,
            Model<? super S> model,
            S state,
            int lightCoords,
            int overlayCoords,
            int tintedColor,
            TextureAtlasSprite sprite,
            PoseStack.Pose sheetedDecalPose
    ) {
        ModelFeatureRenderer.Submit<S> submit = new ModelFeatureRenderer.Submit<>(
                renderType,
                pose,
                model,
                state,
                lightCoords,
                overlayCoords,
                tintedColor,
                sprite,
                sheetedDecalPose
        );
        ((ImmediateLightSubmitExt)(Object)submit).bbe$setLightParameters(parameters);
        return submit;
    }

    private static RenderType outlineRenderType(RenderType renderType) {
        if (renderType.isOutline()) {
            return renderType;
        }

        return renderType.outline().orElse(null);
    }

    public record Parameters(
            BlockAndTintGetter level,
            BlockState blockState,
            BlockPos blockPos,
            SodiumShadeMode shadeMode,
            Matrix4f rootPose,
            Matrix4f lightPose,
            byte optKind,
            boolean movingLighting
    ) {
        public Parameters withRootPose(Matrix4fc rootPose) {
            return new Parameters(
                    this.level,
                    this.blockState,
                    this.blockPos,
                    this.shadeMode,
                    rootPose == null ? null : new Matrix4f(rootPose),
                    this.lightPose,
                    this.optKind,
                    this.movingLighting
            );
        }

        public Parameters withLightPose(Matrix4fc lightPose) {
            return new Parameters(
                    this.level,
                    this.blockState,
                    this.blockPos,
                    this.shadeMode,
                    this.rootPose,
                    lightPose == null ? null : new Matrix4f(lightPose),
                    this.optKind,
                    this.movingLighting
            );
        }
    }

    public record VertexLight(int color, int light) {}

    public interface Scope extends AutoCloseable {
        @Override
        void close();
    }

    public static final class RenderContext {
        private static final int QUAD_VERTICES = 4;

        private final Parameters parameters;
        private final SingleBlockLightDataCache lightCache = new SingleBlockLightDataCache();
        private final LightPipelineProvider lighters = new LightPipelineProvider(this.lightCache);
        private final QuadLightData lightData = new QuadLightData();
        private final LightQuad quad = new LightQuad();
        private final ArrayDeque<Matrix4f> lightPoseStack = new ArrayDeque<>();
        private final ArrayDeque<Matrix4f> modelPartPoseStack = new ArrayDeque<>();
        private final ArrayDeque<LightPoseFrame> poseFrames = new ArrayDeque<>();
        private final Vector3f scratchPosition = new Vector3f();
        private final Vector3f[] scratchModelPositions = {
                new Vector3f(), new Vector3f(), new Vector3f(), new Vector3f()
        };
        private final Vector3f scratchNormal = new Vector3f();
        private final boolean useAmbientOcclusion;
        private final boolean preserveBakedModelNormals;
        private final Matrix4f lightPose;
        private PoseStack.Pose lastPose;
        private ModelPart.Polygon lastPolygon;

        private RenderContext(Parameters parameters) {
            this.parameters = parameters;
            this.lightPose = parameters.lightPose();
            this.lightCache.reset(parameters.blockPos(), parameters.level());
            this.preserveBakedModelNormals = parameters.blockState().is(Blocks.DECORATED_POT);

            this.useAmbientOcclusion = Minecraft.getInstance().options.ambientOcclusion().get()
                    && PlatformBlockAccess.getInstance().getLightEmission(
                            parameters.blockState(),
                            parameters.level(),
                            parameters.blockPos()
                    ) == 0;
        }

        public boolean movingLighting() {
            return this.parameters.movingLighting();
        }

        public void pushPoseFrame(PoseStack poseStack) {
            this.poseFrames.addLast(new LightPoseFrame(poseStack));
        }

        public void popPoseFrame(PoseStack poseStack) {
            if (this.poseFrames.isEmpty()) {
                return;
            }

            LightPoseFrame frame = this.poseFrames.peekLast();
            if (frame.poseStack != poseStack) {
                return;
            }

            frame.close();
            this.poseFrames.removeLast();
        }

        public void applyPartLightPose(PoseStack poseStack, PartPose partPose) {
            if (this.poseFrames.isEmpty()) {
                return;
            }

            LightPoseFrame frame = this.poseFrames.peekLast();
            if (frame.poseStack != poseStack) {
                return;
            }

            frame.replace(this.pushPartLightPose(partPose));
        }

        public Scope pushPartLightPose(PartPose partPose) {
            Matrix4f parent = this.currentLightPose();
            boolean pushedLightPose = parent != null;
            if (pushedLightPose) {
                Matrix4f pose = new Matrix4f(parent);
                applyPartPose(pose, partPose);
                this.lightPoseStack.addLast(pose);
            }

            if (this.preserveBakedModelNormals) {
                Matrix4f parentModelPose = this.modelPartPoseStack.peekLast();
                Matrix4f modelPose = parentModelPose == null ? new Matrix4f() : new Matrix4f(parentModelPose);
                applyPartPose(modelPose, partPose);
                this.modelPartPoseStack.addLast(modelPose);
            }

            return () -> {
                if (this.preserveBakedModelNormals) {
                    this.modelPartPoseStack.removeLast();
                }
                if (pushedLightPose) {
                    this.lightPoseStack.removeLast();
                }
            };
        }

        public VertexLight calculate(
                PoseStack.Pose pose,
                ModelPart.Polygon polygon,
                ModelPart.Vertex currentVertex,
                int color,
                int fallbackLight
        ) {
            ModelPart.Vertex[] vertices = polygon.vertices();
            int vertexIndex = findVertexIndex(vertices, currentVertex);
            if (vertexIndex < 0) {
                return new VertexLight(color, fallbackLight);
            }

            if (this.lastPose != pose || this.lastPolygon != polygon) {
                this.calculateQuadLight(pose, polygon);
                this.lastPose = pose;
                this.lastPolygon = polygon;
            }

            return new VertexLight(
                    scaleRgb(color, this.lightData.br[vertexIndex]),
                    this.lightData.lm[vertexIndex]
            );
        }

        public void prepareLocalQuadLight(
                float x0,
                float y0,
                float z0,
                float x1,
                float y1,
                float z1,
                float x2,
                float y2,
                float z2,
                float x3,
                float y3,
                float z3
        ) {
            this.quad.clear();

            this.setLocalQuadVertex(0, x0, y0, z0);
            this.setLocalQuadVertex(1, x1, y1, z1);
            this.setLocalQuadVertex(2, x2, y2, z2);
            this.setLocalQuadVertex(3, x3, y3, z3);
            this.setBakedModelNormal();

            this.calculatePreparedQuad();
        }

        public VertexLight applyPreparedVertex(
                int vertexIndex,
                int color
        ) {
            return new VertexLight(
                    scaleRgb(color, this.lightData.br[vertexIndex]),
                    this.lightData.lm[vertexIndex]
            );
        }

        private void calculateQuadLight(PoseStack.Pose pose, ModelPart.Polygon polygon) {
            this.quad.clear();

            ModelPart.Vertex[] vertices = polygon.vertices();
            for (int i = 0; i < QUAD_VERTICES; i++) {
                ModelPart.Vertex vertex = vertices[i];
                this.setLocalQuadVertex(
                        i,
                        vertex.worldX(),
                        vertex.worldY(),
                        vertex.worldZ(),
                        pose.pose()
                );
            }
            this.setBakedModelNormal();

            this.calculatePreparedQuad();
        }

        private void setQuadVertex(int index, float x, float y, float z) {
            this.quad.setPos(index, x, y, z);
        }

        private void setLocalQuadVertex(int index, float x, float y, float z) {
            this.setLocalQuadVertex(index, x, y, z, null);
        }

        private void setLocalQuadVertex(int index, float x, float y, float z, Matrix4f fallbackPose) {
            if (this.preserveBakedModelNormals) {
                Vector3f modelPosition = this.scratchModelPositions[index];
                Matrix4f modelPose = this.modelPartPoseStack.peekLast();
                if (modelPose != null) {
                    modelPose.transformPosition(x, y, z, modelPosition);
                } else {
                    modelPosition.set(x, y, z);
                }

                Matrix4f outerPose = this.lightPose != null ? this.lightPose : fallbackPose;
                if (outerPose != null) {
                    outerPose.transformPosition(
                            modelPosition.x(),
                            modelPosition.y(),
                            modelPosition.z(),
                            this.scratchPosition
                    );
                } else {
                    this.scratchPosition.set(modelPosition);
                }

                this.setQuadVertex(
                        index,
                        this.scratchPosition.x(),
                        this.scratchPosition.y(),
                        this.scratchPosition.z()
                );
                return;
            }

            Matrix4f currentPose = this.currentLightPose();
            Matrix4f matrix = currentPose != null ? currentPose : fallbackPose;
            if (matrix != null) {
                matrix.transformPosition(x, y, z, this.scratchPosition);
                this.setQuadVertex(
                        index,
                        this.scratchPosition.x(),
                        this.scratchPosition.y(),
                        this.scratchPosition.z()
                );
                return;
            }

            this.setQuadVertex(index, x, y, z);
        }

        private Matrix4f currentLightPose() {
            Matrix4f current = this.lightPoseStack.peekLast();
            return current == null ? this.lightPose : current;
        }

        private void setBakedModelNormal() {
            if (!this.preserveBakedModelNormals) {
                return;
            }

            Vector3f vertex0 = this.scratchModelPositions[0];
            Vector3f vertex1 = this.scratchModelPositions[1];
            Vector3f vertex2 = this.scratchModelPositions[2];
            Vector3f vertex3 = this.scratchModelPositions[3];

            float edge0X = vertex2.x() - vertex0.x();
            float edge0Y = vertex2.y() - vertex0.y();
            float edge0Z = vertex2.z() - vertex0.z();
            float edge1X = vertex3.x() - vertex1.x();
            float edge1Y = vertex3.y() - vertex1.y();
            float edge1Z = vertex3.z() - vertex1.z();

            float normalX = edge0Y * edge1Z - edge0Z * edge1Y;
            float normalY = edge0Z * edge1X - edge0X * edge1Z;
            float normalZ = edge0X * edge1Y - edge0Y * edge1X;
            float length = (float)Math.sqrt(normalX * normalX + normalY * normalY + normalZ * normalZ);
            if (length != 0.0F && length != 1.0F) {
                normalX /= length;
                normalY /= length;
                normalZ /= length;
            }
            this.scratchNormal.set(normalX, normalY, normalZ);

            for (int vertexIndex = 0; vertexIndex < QUAD_VERTICES; vertexIndex++) {
                this.quad.setNormal(
                        vertexIndex,
                        this.scratchNormal.x(),
                        this.scratchNormal.y(),
                        this.scratchNormal.z()
                );
            }
        }

        private static void applyPartPose(Matrix4f matrix, PartPose pose) {
            matrix.translate(pose.x() / 16.0F, pose.y() / 16.0F, pose.z() / 16.0F);

            if (pose.xRot() != 0.0F || pose.yRot() != 0.0F || pose.zRot() != 0.0F) {
                matrix.rotateZYX(pose.zRot(), pose.yRot(), pose.xRot());
            }

            if (pose.xScale() != 1.0F || pose.yScale() != 1.0F || pose.zScale() != 1.0F) {
                matrix.scale(pose.xScale(), pose.yScale(), pose.zScale());
            }
        }

        private void calculatePreparedQuad() {
            Direction resolvedLightFace = this.quad.getLightFace();

            this.lighters.getLighter(this.useAmbientOcclusion ? LightMode.SMOOTH : LightMode.FLAT).calculate(
                    this.quad,
                    this.parameters.blockPos(),
                    this.lightData,
                    this.quad.getCullFace(),
                    resolvedLightFace,
                    true,
                    this.parameters.shadeMode() == SodiumShadeMode.ENHANCED
            );
        }

        private static int findVertexIndex(ModelPart.Vertex[] vertices, ModelPart.Vertex currentVertex) {
            for (int i = 0; i < vertices.length; i++) {
                if (vertices[i] == currentVertex) {
                    return i;
                }
            }
            return -1;
        }

        private static int scaleRgb(int color, float scale) {
            int red = scaleComponent((color >>> 16) & 0xFF, scale);
            int green = scaleComponent((color >>> 8) & 0xFF, scale);
            int blue = scaleComponent(color & 0xFF, scale);

            return (color & 0xFF000000) | (red << 16) | (green << 8) | blue;
        }

        private static int scaleComponent(int component, float scale) {
            int scaled = (int)(component * scale);
            if (scaled < 0) {
                return 0;
            }
            return Math.min(scaled, 0xFF);
        }

        private static final class LightPoseFrame {
            private final PoseStack poseStack;
            private Scope scope;

            private LightPoseFrame(PoseStack poseStack) {
                this.poseStack = poseStack;
            }

            private void replace(Scope nextScope) {
                this.close();
                this.scope = nextScope;
            }

            private void close() {
                if (this.scope != null) {
                    this.scope.close();
                    this.scope = null;
                }
            }
        }

    }

    private static final class LightQuad extends MutableQuadViewImpl {
        private LightQuad() {
            this.data = new int[EncodingFormat.TOTAL_STRIDE];
            this.baseIndex = 0;
            this.clear();
        }

        @Override
        public void emitDirectly() {}
    }
}
