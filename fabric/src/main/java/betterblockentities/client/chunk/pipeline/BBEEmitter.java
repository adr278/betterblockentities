package betterblockentities.client.chunk.pipeline;

/* local */
import betterblockentities.mixin.sodium.pipeline.AbstractBlockRenderContextAccessor;
import betterblockentities.client.chunk.util.QuadTransform;

/* minecraft */
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.Material;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;

/* mojang */
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Transformation;

/* sodium */
import net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderer;
import net.caffeinemc.mods.sodium.client.render.model.MutableQuadViewImpl;
import net.caffeinemc.mods.sodium.client.services.PlatformModelEmitter;
import net.caffeinemc.mods.sodium.client.render.helper.ModelHelper;
import net.caffeinemc.mods.sodium.client.render.model.AmbientOcclusionMode;
import net.caffeinemc.mods.sodium.client.render.model.SodiumShadeMode;
import net.caffeinemc.mods.sodium.client.services.PlatformBlockAccess;

/* java/misc */
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class BBEEmitter {
    /* quad tag, forcing quad splitting off */
    public static int NO_QUAD_SPLITTING = "BBE-TS-QUAD-NO-SPLIT".hashCode();

    public static final int MAX_FACE_INDEX = 6;
    public static final int QUAD_VERTICES = 4;
    private final Vector3f scratchVector = new Vector3f();

    /* allocate PoseStack once and reuse over this emitter's lifecycle */
    private final PoseStack poseStack = new PoseStack();

    /* sodium block render context */
    private final AbstractBlockRenderContextAccessor sodiumContext;

    /* quad render data */
    private Material material;
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
    }

    public void emit(ArrayList<BlockModelPart> partsLocal, Predicate<@Nullable Direction> cullTest, PlatformModelEmitter.Bufferer bufferer) {
        for(int i = 0; i < partsLocal.size(); ++i) {
            BlockModelPart part = partsLocal.get(i);
            bufferer.emit(part, cullTest, MutableQuadViewImpl::emitDirectly);
        }
    }

    public void buffer(BlockModelPart part, Predicate<Direction> cullTest, Consumer<MutableQuadViewImpl> sodiumEmitterConsumer) {
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
            poseStack.mulPose(this.b3dtransformation.getMatrix());

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

    public void setMaterial(Material material) {
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
