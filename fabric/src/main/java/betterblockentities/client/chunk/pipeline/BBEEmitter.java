package betterblockentities.client.chunk.pipeline;

/* local */
import betterblockentities.client.chunk.pipeline.shelf.CacheKeys;
import betterblockentities.client.chunk.pipeline.shelf.GeometryBaker;
import betterblockentities.client.chunk.util.QuadTransform;
import betterblockentities.mixin.sodium.pipeline.AbstractBlockRenderContextAccessor;
import betterblockentities.mixin.sodium.pipeline.BlockRendererAccessor;

/* minecraft */
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.TriState;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.LightLayer;

/* sodium */
import net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderer;
import net.caffeinemc.mods.sodium.client.render.helper.ModelHelper;
import net.caffeinemc.mods.sodium.client.render.model.*;
import net.caffeinemc.mods.sodium.client.services.PlatformBlockAccess;
import net.caffeinemc.mods.sodium.client.services.PlatformModelEmitter;

/* mojang */
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Transformation;

/* java/misc */
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class BBEEmitter {
    /* quad tag, forcing quad splitting off */
    public static int NO_QUAD_SPLITTING = "BBE-TS-QUAD-NO-SPLIT".hashCode();

    public static final int MAX_FACE_INDEX = 6;
    public static final int QUAD_VERTICES = 4;
    private final Vector3f scratchVector = new Vector3f();
    private final Vector3f scratchPos0 = new Vector3f();
    private final Vector3f scratchPos1 = new Vector3f();
    private final Vector3f scratchPos2 = new Vector3f();
    private final Vector3f scratchPos3 = new Vector3f();
    private final Vector3f scratchEdge0 = new Vector3f();
    private final Vector3f scratchEdge1 = new Vector3f();
    private final Vector3f scratchExpectedNormal = new Vector3f();

    /* allocate PoseStack once and reuse over this emitter's lifecycle */
    private final PoseStack poseStack = new PoseStack();

    /* sodium block render context */
    private final AbstractBlockRenderContextAccessor sodiumContext;
    private final BlockRendererAccessor sodiumBlockRenderer;

    /* quad render data */
    private SpriteId material;
    private ChunkSectionLayer renderType;
    private TextureAtlasSprite sprite;
    private Quaternionf rotation;
    private Transformation b3dtransformation;
    private AmbientOcclusionMode aoMode;
    private QuadSplittingMode quadSplittingMode;
    private float xRot = 0;
    private float yRot = 0;
    private float zRot = 0;
    private int color = -1;

    public BBEEmitter(BlockRenderer sodiumBlockRenderer) {
        this.sodiumContext = (AbstractBlockRenderContextAccessor)sodiumBlockRenderer;
        this.sodiumBlockRenderer = (BlockRendererAccessor)(Object)sodiumBlockRenderer;
    }

    public void emit(ArrayList<BlockStateModelPart> partsLocal, Predicate<@Nullable Direction> cullTest, PlatformModelEmitter.Bufferer bufferer) {
        for(int i = 0; i < partsLocal.size(); ++i) {
            BlockStateModelPart part = partsLocal.get(i);
            bufferer.emit(part, cullTest, MutableQuadViewImpl::emitDirectly);
        }
    }

    public void emitPackedQuad(
            Matrix4f pose,
            GeometryBaker.PackedQuad quad,
            int color,
            int light,
            ChunkSectionLayer layer,
            boolean useAmbientOcclusion,
            boolean useFaceCulling
    ) {
        TextureAtlasSprite resolvedSprite = quad.spriteForCacheOrNull() != null ? quad.spriteForCacheOrNull() : quad.sprite();
        if (resolvedSprite == null) return;

        MutableQuadViewImpl sodiumEmitter = sodiumContext.getEmitterInvoke();
        int resolvedColor = color == CacheKeys.NO_TINT ? -1 : color;
        long uv0 = quad.uv0();
        long uv1 = quad.uv1();
        long uv2 = quad.uv2();
        long uv3 = quad.uv3();

        Vector3f p0 = pose.transformPosition(quad.x0(), quad.y0(), quad.z0(), this.scratchPos0);
        Vector3f p1 = pose.transformPosition(quad.x1(), quad.y1(), quad.z1(), this.scratchPos1);
        Vector3f p2 = pose.transformPosition(quad.x2(), quad.y2(), quad.z2(), this.scratchPos2);
        Vector3f p3 = pose.transformPosition(quad.x3(), quad.y3(), quad.z3(), this.scratchPos3);

        Vector3f expectedNormal = this.scratchExpectedNormal;
        this.scratchEdge0.set(p1).sub(p0);
        this.scratchEdge1.set(p2).sub(p0);
        this.scratchEdge0.cross(this.scratchEdge1, expectedNormal);

        if (expectedNormal.lengthSquared() < 1.0E-8F) {
            pose.transformDirection(
                    quad.dir().getStepX(),
                    quad.dir().getStepY(),
                    quad.dir().getStepZ(),
                    expectedNormal
            );
        }

        expectedNormal.normalize();
        Direction expectedFace = Direction.getApproximateNearest(expectedNormal.x, expectedNormal.y, expectedNormal.z);

        sodiumEmitter.clear();
        sodiumEmitter.cachedSprite(resolvedSprite);
        sodiumEmitter.setRenderType(layer);
        sodiumEmitter.setAmbientOcclusion(useAmbientOcclusion ? TriState.DEFAULT : TriState.FALSE);
        sodiumEmitter.setDiffuseShade(quad.shade());
        sodiumEmitter.setShadeMode(SodiumShadeMode.ENHANCED);
        sodiumEmitter.setTintIndex(-1);

        emitPackedVertex(sodiumEmitter, 0, p0, uv0, resolvedColor, light);
        emitPackedVertex(sodiumEmitter, 1, p1, uv1, resolvedColor, light);
        emitPackedVertex(sodiumEmitter, 2, p2, uv2, resolvedColor, light);
        emitPackedVertex(sodiumEmitter, 3, p3, uv3, resolvedColor, light);

        sodiumEmitter.setCullFace(useFaceCulling ? expectedFace : null);
        sodiumEmitter.setNominalFace(expectedFace);

        sodiumEmitter.emitDirectly();
        sodiumEmitter.clear();
    }

    public int packedLightAt(BlockPos pos) {
        int blockLight = this.sodiumContext.getLevel().getBrightness(LightLayer.BLOCK, pos);
        int skyLight = this.sodiumContext.getLevel().getBrightness(LightLayer.SKY, pos);
        return LightCoordsUtil.pack(blockLight, skyLight);
    }

    public float posOffsetX() { return this.sodiumBlockRenderer.getPosOffset().x; }

    public float posOffsetY() { return this.sodiumBlockRenderer.getPosOffset().y; }

    public float posOffsetZ() { return this.sodiumBlockRenderer.getPosOffset().z; }

    public void withWorldContext(BlockPos pos, Runnable action) {
        BlockPos oldPos = this.sodiumContext.getPos();
        BlockState oldState = this.sodiumContext.getState();

        try {
            this.sodiumContext.setPos(pos);
            this.sodiumContext.setState(this.sodiumContext.getLevel().getBlockState(pos));
            this.sodiumContext.prepareAoInfoInvoke(true);
            this.sodiumContext.prepareCullingInvoke(false);
            action.run();
        } finally {
            this.sodiumContext.setPos(oldPos);
            this.sodiumContext.setState(oldState);
            this.sodiumContext.prepareAoInfoInvoke(true);
            this.sodiumContext.prepareCullingInvoke(false);
        }
    }

    private void emitPackedVertex(
            MutableQuadViewImpl sodiumEmitter,
            int vertex,
            Vector3f transformedPos,
            long uv,
            int color,
            int light
    ) {
        sodiumEmitter.setPos(vertex, transformedPos.x, transformedPos.y, transformedPos.z);
        sodiumEmitter.setUV(vertex, UVPair.unpackU(uv), UVPair.unpackV(uv));
        sodiumEmitter.setColor(vertex, color);
        sodiumEmitter.setLight(vertex, light);
    }

    public void buffer(BlockStateModelPart part, Predicate<Direction> cullTest, Consumer<MutableQuadViewImpl> sodiumEmitterConsumer) {
        final MutableQuadViewImpl sodiumEmitter = sodiumContext.getEmitterInvoke();

        sodiumContext.prepareAoInfoInvoke(part.useAmbientOcclusion());

        for (int i = 0; i <= MAX_FACE_INDEX; ++i) {
            Direction cullFace = ModelHelper.faceFromIndex(i);
            if (cullTest.test(cullFace)) {
                continue;
            }

            List<BakedQuad> quads = part.getQuads(cullFace);

            AmbientOcclusionMode sodiumAO = PlatformBlockAccess.getInstance().usesAmbientOcclusion(
                    part, sodiumContext.getState(), this.renderType, sodiumContext.getSlice(), sodiumContext.getPos()
            );

            for (int j = 0, count = quads.size(); j < count; ++j) {
                BakedQuad quad = quads.get(j);

                sodiumEmitter.fromBakedQuad(quad);
                sodiumEmitter.setCullFace(cullFace);
                sodiumEmitter.setShadeMode(SodiumShadeMode.ENHANCED);

                /* modify sodium emitter data */
                applyAmbientOcclusionMode(sodiumEmitter, sodiumAO);
                applyRenderType(sodiumEmitter);
                applySprite(sodiumEmitter);
                applyTransformation(sodiumEmitter);
                applyColor(sodiumEmitter);
                applyQuadSplittingMode(sodiumEmitter);

                sodiumEmitterConsumer.accept(sodiumEmitter);
            }
        }
        sodiumEmitter.clear();
    }

    private void applyRenderType(MutableQuadViewImpl sodiumEmitter) {
        if (this.renderType != null) {
            sodiumEmitter.setRenderType(this.renderType);
        }
    }

    private void applySprite(MutableQuadViewImpl sodiumEmitter) {
        if (this.material != null) {
            this.sprite = QuadTransform.getBlockSprite(this.material.texture());
        }

        if (this.sprite != null) {
            QuadTransform.remapSprite(this.sprite, sodiumEmitter);
        }
    }

    private void applyTransformation(MutableQuadViewImpl sodiumEmitter) {
        /* blaze3d path */
        if (this.b3dtransformation != null) {
            poseStack.pushPose();
            poseStack.mulPose(this.b3dtransformation);

            for (int v = 0; v < QUAD_VERTICES; ++v) {
                Matrix4f matrix = poseStack.last().pose();

                float vX = sodiumEmitter.getX(v);
                float vY = sodiumEmitter.getY(v);
                float vZ = sodiumEmitter.getZ(v);

                Vector3f pos = matrix.transformPosition(vX, vY, vZ, this.scratchVector);

                sodiumEmitter.setPos(v, pos.x, pos.y, pos.z);
            }
            poseStack.popPose();
        }

        /* quaternion rotation */
        if (this.rotation != null) {
            QuadTransform.rotate(sodiumEmitter, this.rotation, scratchVector);
        }

        /* ModelPart style Euler rotation */
        if (this.xRot != 0 || this.yRot != 0 || this.zRot != 0) {
            QuadTransform.rotateXYZ(sodiumEmitter, this.xRot, this.yRot, this.zRot);
        }
    }

    private void applyColor(MutableQuadViewImpl sodiumEmitter) {
        if (this.color != -1) {
            for (int v = 0; v < QUAD_VERTICES; ++v) {
                sodiumEmitter.setColor(v, this.color);
            }
        }
    }

    private void applyAmbientOcclusionMode(MutableQuadViewImpl sodiumEmitter, AmbientOcclusionMode sodiumAO) {
        if (this.aoMode == null) {
            sodiumEmitter.setAmbientOcclusion(sodiumAO.toTriState());
        }
        else {
            sodiumEmitter.setAmbientOcclusion(this.aoMode.toTriState());
        }
    }

    public void applyQuadSplittingMode(MutableQuadViewImpl sodiumEmitter) {
        if (this.quadSplittingMode == QuadSplittingMode.NONE) {
            sodiumEmitter.setTag(NO_QUAD_SPLITTING);
        }
    }

    public void setMaterial(SpriteId material) {
        this.material = material;
    }

    public void setRenderType(ChunkSectionLayer layer) {
        this.renderType = layer;
    }

    public void setTransformation(Transformation transformation) {
        this.b3dtransformation = transformation;
    }

    public void setRotation(float x, float y, float z) {
        this.xRot = x;
        this.yRot = y;
        this.zRot = z;
    }

    public void setRotation(Quaternionf quaternion) {
        this.rotation = quaternion;
    }

    public void setColor(int color) {
        this.color = color;
    }

    public void setSprite(TextureAtlasSprite sprite) {
        this.sprite = sprite;
    }

    public void setAmbientOcclusionMode(AmbientOcclusionMode mode) {
        this.aoMode = mode;
    }

    public void setSplittingMode(QuadSplittingMode mode) {
        this.quadSplittingMode = mode;
    }

    public void clear() {
        this.material = null;
        this.renderType = null;
        this.sprite = null;
        this.rotation = null;
        this.b3dtransformation = null;
        this.aoMode = null;
        this.quadSplittingMode = null;
        this.xRot = 0;
        this.yRot = 0;
        this.zRot = 0;
        this.color = -1;
    }

    /* NONE = No splitting, DEFERRED = leave it to sodium to decide */
    public enum QuadSplittingMode {
        NONE,
        DEFERRED
    }
}
