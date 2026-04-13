package betterblockentities.client.chunk.pipeline.itemframe;

/* local */
import betterblockentities.client.chunk.pipeline.BBEEmitter;

/* minecraft */
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.entity.ItemFrameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.phys.Vec3;

/* joml */
import org.joml.Matrix4f;

public final class ItemFrameTerrainEmitter {
    private static final float FRAME_FACE_OFFSET = 0.46874F;
    private static final float INVISIBLE_CONTENT_DEPTH = 0.5F;
    private static final float VISIBLE_CONTENT_DEPTH = 0.4375F;
    private static final float ITEM_SCALE = 0.5F;

    public static void emitForSection(BlockPos anyBlockInSection, BBEEmitter emitter) {
        int sectionX = SectionPos.blockToSectionCoord(anyBlockInSection.getX());
        int sectionY = SectionPos.blockToSectionCoord(anyBlockInSection.getY());
        int sectionZ = SectionPos.blockToSectionCoord(anyBlockInSection.getZ());

        int localX = anyBlockInSection.getX() & 15;
        int localY = anyBlockInSection.getY() & 15;
        int localZ = anyBlockInSection.getZ() & 15;

        float offsetX = emitter.posOffsetX() - localX;
        float offsetY = emitter.posOffsetY() - localY;
        float offsetZ = emitter.posOffsetZ() - localZ;

        float anchorX = anyBlockInSection.getX() + offsetX;
        float anchorY = anyBlockInSection.getY() + offsetY;
        float anchorZ = anyBlockInSection.getZ() + offsetZ;

        BlockPos[] supports = ItemFrameSectionRegistry.supportsForSection(sectionX, sectionY, sectionZ);
        for (BlockPos supportPos : supports) emitFramesForSupportWithAnchor(supportPos, anchorX, anchorY, anchorZ, emitter);
    }

    private static void emitFramesForSupportWithAnchor(
            BlockPos supportPos,
            float anchorX,
            float anchorY,
            float anchorZ,
            BBEEmitter emitter
    ) {
        ItemFrame[] frames = ItemFrameSectionRegistry.framesForSupport(supportPos);

        for (ItemFrame frame : frames) {
            if (frame.level() instanceof ClientLevel clientLevel) {
                Entity live = clientLevel.getEntity(frame.getId());
                if (live != frame) {
                    ItemFrameSectionRegistry.remove(frame.getId());
                    continue;
                }
            }

            if (frame.isRemoved() || !ItemFrameEligibility.isFrameMeshSupported(frame)) {
                ItemFrameSectionRegistry.remove(frame.getId());
                continue;
            }

            ItemFrameEligibility.Evaluation evaluation = ItemFrameEligibility.evaluateForTerrainEmission(frame);
            BlockPos framePos = frame.getPos();
            emitter.withWorldContext(framePos, () -> emitFrame(frame, anchorX, anchorY, anchorZ, evaluation, emitter));
        }
    }

    private static void emitFrame(
            ItemFrame frame,
            float anchorX,
            float anchorY,
            float anchorZ,
            ItemFrameEligibility.Evaluation evaluation,
            BBEEmitter emitter
    ) {
        boolean glowFrame = frame.is(EntityType.GLOW_ITEM_FRAME);
        boolean invisible = frame.isInvisible();
        Vec3 lightProbe = frame.getLightProbePosition(1.0F);
        BlockPos lightProbePos = BlockPos.containing(lightProbe);
        int packedLight = emitter.packedLightAt(lightProbePos);
        if (glowFrame) {
            int glowBlock = Math.max(ItemFrameRenderer.GLOW_FRAME_BRIGHTNESS, LightCoordsUtil.block(packedLight));
            packedLight = LightCoordsUtil.withBlock(packedLight, glowBlock);
        }
        int packedItemLight = glowFrame ? LightCoordsUtil.FULL_BRIGHT : packedLight;

        Matrix4f framePose = new Matrix4f()
                .translate(
                        (float) (frame.position().x - anchorX),
                        (float) (frame.position().y - anchorY),
                        (float) (frame.position().z - anchorZ)
                );
        applyFacingTransform(framePose, frame.getDirection());

        if (!invisible) {
            ItemFrameItemModelBuilder.CapturedMesh frameMesh = ItemFrameModelCapture.getFrameMesh(glowFrame);
            if (frameMesh != null) {
                emitCapturedMesh(
                        frameMesh,
                        framePose,
                        packedLight,
                        emitter
                );
            }
        }

        if (evaluation.contentRenderMode() != ItemFrameContentRenderMode.NONE) return;

        ItemFrameItemModelBuilder.CapturedMesh itemMesh = evaluation.itemCapture().mesh();
        if (itemMesh == null || itemMesh.isEmpty()) return;

        Matrix4f contentPose = new Matrix4f(framePose);
        translateToContentPlane(contentPose, invisible);
        applyItemTransform(contentPose, frame.getRotation());

        emitCapturedMesh(
                itemMesh,
                contentPose,
                packedItemLight,
                emitter
        );
    }

    private static void emitCapturedMesh(
            ItemFrameItemModelBuilder.CapturedMesh mesh,
            Matrix4f pose,
            int baseLight,
            BBEEmitter emitter
    ) {
        for (ItemFrameItemModelBuilder.LayeredQuad layeredQuad : mesh.quads()) {
            emitter.emitPackedQuad(
                    pose,
                    layeredQuad.quad(),
                    layeredQuad.color(),
                    baseLight,
                    layeredQuad.layer(),
                    true,
                    false
            );
        }
    }

    private static void applyFacingTransform(Matrix4f pose, Direction direction) {
        pose.translate(
                direction.getStepX() * FRAME_FACE_OFFSET,
                direction.getStepY() * FRAME_FACE_OFFSET,
                direction.getStepZ() * FRAME_FACE_OFFSET
        );

        float xRot;
        float yRot;

        if (direction.getAxis().isHorizontal()) {
            xRot = 0.0F;
            yRot = 180.0F - direction.toYRot();
        } else {
            xRot = -90.0F * direction.getAxisDirection().getStep();
            yRot = 180.0F;
        }

        pose.rotateX((float) Math.toRadians(xRot));
        pose.rotateY((float) Math.toRadians(yRot));
    }

    private static void translateToContentPlane(Matrix4f pose, boolean invisible) {
        pose.translate(0.0F, 0.0F, invisible ? INVISIBLE_CONTENT_DEPTH : VISIBLE_CONTENT_DEPTH);
    }

    private static void applyItemTransform(Matrix4f pose, int rotation) {
        pose.rotateZ((float) Math.toRadians(rotation * 360.0F / 8.0F));
        pose.scale(ITEM_SCALE, ITEM_SCALE, ITEM_SCALE);
    }
}
