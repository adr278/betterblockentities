package betterblockentities.client.chunk.pipeline.itemframe;

/* mojang */
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

/* minecraft */
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.level.saveddata.maps.MapId;

/* joml */
import org.joml.Vector3f;

public final class ItemFrameSectionMapSurfaceRenderer {
    private record QuadGeometry(
            float[] positions,
            float normalX,
            float normalY,
            float normalZ
    ) {}

    private static final class SurfaceSnapshot {
        private final ItemFrameMapIndex.Entry[] visibleEntries;
        private final int atlasAssignmentVersion;
        private final int surfaceRegistryVersion;

        private final double[] entityX;
        private final double[] entityY;
        private final double[] entityZ;

        private final float[] u0;
        private final float[] v0;
        private final float[] u1;
        private final float[] v1;

        private final int[] light;
        private final QuadGeometry[] geometry;

        private int count;

        private SurfaceSnapshot(
                ItemFrameMapIndex.Entry[] visibleEntries,
                int atlasAssignmentVersion,
                int surfaceRegistryVersion,
                int capacity
        ) {
            this.visibleEntries = visibleEntries;
            this.atlasAssignmentVersion = atlasAssignmentVersion;
            this.surfaceRegistryVersion = surfaceRegistryVersion;

            this.entityX = new double[capacity];
            this.entityY = new double[capacity];
            this.entityZ = new double[capacity];

            this.u0 = new float[capacity];
            this.v0 = new float[capacity];
            this.u1 = new float[capacity];
            this.v1 = new float[capacity];

            this.light = new int[capacity];
            this.geometry = new QuadGeometry[capacity];
        }

        private boolean matches(
                ItemFrameMapIndex.Entry[] visibleEntries,
                int atlasAssignmentVersion,
                int surfaceRegistryVersion
        ) {
            return this.visibleEntries == visibleEntries
                    && this.atlasAssignmentVersion == atlasAssignmentVersion
                    && this.surfaceRegistryVersion == surfaceRegistryVersion;
        }

        private void add(
                double entityX,
                double entityY,
                double entityZ,
                float u0,
                float v0,
                float u1,
                float v1,
                int light,
                QuadGeometry geometry
        ) {
            int index = this.count++;

            this.entityX[index] = entityX;
            this.entityY[index] = entityY;
            this.entityZ[index] = entityZ;

            this.u0[index] = u0;
            this.v0[index] = v0;
            this.u1[index] = u1;
            this.v1[index] = v1;

            this.light[index] = light;
            this.geometry[index] = geometry;
        }
    }

    private static final QuadGeometry[][][] QUAD_GEOMETRIES = bakeQuadGeometries();
    private static final RenderType MAP_SURFACE_RENDER_TYPE = RenderTypes.text(MapAtlasManager.ATLAS_TEXTURE);

    private static volatile SurfaceSnapshot cachedSnapshot =
            new SurfaceSnapshot(new ItemFrameMapIndex.Entry[0], Integer.MIN_VALUE, Integer.MIN_VALUE, 0);

    private ItemFrameSectionMapSurfaceRenderer() {}

    public static void submitUploadedMapSurfaces(
            PoseStack poseStack,
            SubmitNodeCollector submitNodeCollector,
            CameraRenderState camera
    ) {
        if (!ItemFrameEligibility.optimizationEnabled()) return;
        if (MapAtlasManager.atlasNullable() == null) return;

        ItemFrameMapIndex.Entry[] visibleEntries = MapPageCache.visibleEntrySnapshot();
        if (visibleEntries.length == 0) return;

        int atlasAssignmentVersion = MapPageCache.atlasAssignmentVersion();
        int surfaceRegistryVersion = ItemFrameMapSurfaceRegistry.version();

        SurfaceSnapshot snapshot = cachedSnapshot;
        if (!snapshot.matches(visibleEntries, atlasAssignmentVersion, surfaceRegistryVersion)) {
            snapshot = buildSurfaceSnapshot(visibleEntries, atlasAssignmentVersion, surfaceRegistryVersion);
            cachedSnapshot = snapshot;
        }

        if (snapshot.count == 0) return;

        double camX = camera.pos.x();
        double camY = camera.pos.y();
        double camZ = camera.pos.z();

        SurfaceSnapshot submittedSnapshot = snapshot;
        int submitCount = submittedSnapshot.count;

        submitNodeCollector.submitCustomGeometry(
                poseStack,
                MAP_SURFACE_RENDER_TYPE,
                (ignoredPose, consumer) -> {
                    for (int i = 0; i < submitCount; i++) {
                        submitMapSurface(submittedSnapshot, i, camX, camY, camZ, consumer);
                    }
                }
        );
    }

    private static SurfaceSnapshot buildSurfaceSnapshot(
            ItemFrameMapIndex.Entry[] visibleEntries,
            int atlasAssignmentVersion,
            int surfaceRegistryVersion
    ) {
        SurfaceSnapshot snapshot = new SurfaceSnapshot(
                visibleEntries,
                atlasAssignmentVersion,
                surfaceRegistryVersion,
                visibleEntries.length
        );

        int lastMapNumericId = Integer.MIN_VALUE;
        MapAtlasRef lastAtlasRef = null;

        for (ItemFrameMapIndex.Entry mapEntry : visibleEntries) {
            ItemFrameMapSurfaceRegistry.ActiveSurfaceState surfaceState =
                    ItemFrameMapSurfaceRegistry.get(mapEntry.entityId());
            if (surfaceState == null) continue;

            MapId mapId = mapEntry.mapId();
            if (!surfaceState.mapId().equals(mapId)) continue;

            int mapNumericId = mapId.id();
            MapAtlasRef atlasRef;
            if (mapNumericId == lastMapNumericId) {
                atlasRef = lastAtlasRef;
            } else {
                atlasRef = resolveAtlasRef(mapId);
                lastMapNumericId = mapNumericId;
                lastAtlasRef = atlasRef;
            }
            if (atlasRef == null) continue;

            snapshot.add(
                    mapEntry.entityPos().x,
                    mapEntry.entityPos().y,
                    mapEntry.entityPos().z,
                    atlasRef.u0(),
                    atlasRef.v0(),
                    atlasRef.u1(),
                    atlasRef.v1(),
                    surfaceState.mapLight(),
                    geometryFor(mapEntry)
            );
        }

        return snapshot;
    }

    public static void invalidateSnapshot() {
        cachedSnapshot = new SurfaceSnapshot(
                new ItemFrameMapIndex.Entry[0],
                Integer.MIN_VALUE,
                Integer.MIN_VALUE,
                0
        );
    }

    private static QuadGeometry[][][] bakeQuadGeometries() {
        QuadGeometry[][][] geometries = new QuadGeometry[Direction.values().length][2][4];

        for (Direction direction : Direction.values()) {
            for (int invisibleIndex = 0; invisibleIndex < 2; invisibleIndex++) {
                boolean invisible = invisibleIndex == 1;
                for (int rotation = 0; rotation < 4; rotation++) {
                    PoseStack poseStack = new PoseStack();
                    ItemFrameRenderHelper.applyFacingPose(poseStack, direction);
                    ItemFrameRenderHelper.translateToContentPlane(poseStack, invisible);
                    ItemFrameRenderHelper.applyMapPose(poseStack, rotation);

                    PoseStack.Pose pose = poseStack.last();
                    float[] positions = new float[12];
                    writeVertexPosition(pose, positions, 0, 0.0F, 128.0F);
                    writeVertexPosition(pose, positions, 3, 128.0F, 128.0F);
                    writeVertexPosition(pose, positions, 6, 128.0F, 0.0F);
                    writeVertexPosition(pose, positions, 9, 0.0F, 0.0F);

                    Vector3f normal = pose.transformNormal(0.0F, 0.0F, 1.0F, new Vector3f());
                    geometries[direction.ordinal()][invisibleIndex][rotation] = new QuadGeometry(
                            positions,
                            normal.x(),
                            normal.y(),
                            normal.z()
                    );
                }
            }
        }

        return geometries;
    }

    private static void writeVertexPosition(
            PoseStack.Pose pose,
            float[] positions,
            int index,
            float x,
            float y
    ) {
        Vector3f transformed = pose.pose().transformPosition(x, y, -0.02F, new Vector3f());
        positions[index] = transformed.x();
        positions[index + 1] = transformed.y();
        positions[index + 2] = transformed.z();
    }

    private static QuadGeometry geometryFor(ItemFrameMapIndex.Entry mapEntry) {
        return QUAD_GEOMETRIES
                [mapEntry.facing().ordinal()]
                [mapEntry.invisible() ? 1 : 0]
                [Math.floorMod(mapEntry.rotation(), 4)];
    }

    private static MapAtlasRef resolveAtlasRef(MapId mapId) {
        return MapPageCache.peekAtlasRefFast(mapId);
    }

    private static void submitMapSurface(
            SurfaceSnapshot snapshot,
            int index,
            double camX,
            double camY,
            double camZ,
            VertexConsumer consumer
    ) {
        QuadGeometry geometry = snapshot.geometry[index];
        float[] positions = geometry.positions();

        float baseX = (float) (snapshot.entityX[index] - camX);
        float baseY = (float) (snapshot.entityY[index] - camY);
        float baseZ = (float) (snapshot.entityZ[index] - camZ);

        float normalX = geometry.normalX();
        float normalY = geometry.normalY();
        float normalZ = geometry.normalZ();

        int light = snapshot.light[index];

        putVertex(
                consumer,
                baseX + positions[0],
                baseY + positions[1],
                baseZ + positions[2],
                snapshot.u0[index],
                snapshot.v1[index],
                light,
                normalX,
                normalY,
                normalZ
        );
        putVertex(
                consumer,
                baseX + positions[3],
                baseY + positions[4],
                baseZ + positions[5],
                snapshot.u1[index],
                snapshot.v1[index],
                light,
                normalX,
                normalY,
                normalZ
        );
        putVertex(
                consumer,
                baseX + positions[6],
                baseY + positions[7],
                baseZ + positions[8],
                snapshot.u1[index],
                snapshot.v0[index],
                light,
                normalX,
                normalY,
                normalZ
        );
        putVertex(
                consumer,
                baseX + positions[9],
                baseY + positions[10],
                baseZ + positions[11],
                snapshot.u0[index],
                snapshot.v0[index],
                light,
                normalX,
                normalY,
                normalZ
        );
    }

    private static void putVertex(
            VertexConsumer consumer,
            float x,
            float y,
            float z,
            float u,
            float v,
            int light,
            float normalX,
            float normalY,
            float normalZ
    ) {
        consumer.addVertex(x, y, z)
                .setColor(-1)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(normalX, normalY, normalZ);
    }
}
