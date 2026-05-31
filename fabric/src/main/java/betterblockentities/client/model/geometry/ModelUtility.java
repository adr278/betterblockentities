package betterblockentities.client.model.geometry;

import betterblockentities.mixin.model.modelpart.ModelPartCubeAccessor;

/* minecraft */
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.core.Direction;

/* mojang */
import com.mojang.blaze3d.vertex.PoseStack;

/* java/misc */
import org.joml.Vector3f;
import org.joml.Vector3fc;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility for baking ModelPart trees into reusable quad templates.
 */
public class ModelUtility {

    public static GeometryRegistry.ModelTemplate bakeTemplate(final ModelPart root, final PoseStack stack) {
        final Map<String, List<GeometryRegistry.QuadTemplate>> partQuads = new HashMap<>();
        final List<GeometryRegistry.QuadTemplate> allQuads = new ArrayList<>();

        root.visit(stack, (pose, path, index, cube) -> {
            final String topLevelPart = topLevelPart(path);
            final List<GeometryRegistry.QuadTemplate> bucket = partQuads.computeIfAbsent(topLevelPart, ignored -> new ArrayList<>());

            for (ModelPart.Polygon polygon : ((ModelPartCubeAccessor) (Object) cube).getPolygons()) {
                final GeometryRegistry.QuadTemplate quad = bakePolygon(pose, polygon);
                if (quad == null) {
                    continue;
                }

                allQuads.add(quad);
                bucket.add(quad);
            }
        });

        if (partQuads.isEmpty() && !allQuads.isEmpty()) {
            partQuads.put("root", allQuads);
        }

        return new GeometryRegistry.ModelTemplate(allQuads, partQuads);
    }

    public static List<GeometryRegistry.QuadTemplate> bakePart(final ModelPart part, final PoseStack stack) {
        final List<GeometryRegistry.QuadTemplate> quads = new ArrayList<>();

        part.visit(stack, (pose, path, index, cube) -> {
            for (ModelPart.Polygon polygon : ((ModelPartCubeAccessor) (Object) cube).getPolygons()) {
                final GeometryRegistry.QuadTemplate baked = bakePolygon(pose, polygon);
                if (baked != null) {
                    quads.add(baked);
                }
            }
        });

        return quads;
    }

    private static GeometryRegistry.QuadTemplate bakePolygon(final PoseStack.Pose pose, final ModelPart.Polygon polygon) {
        final ModelPart.Vertex[] vertices = polygon.vertices;
        if (vertices.length != 4) {
            return null;
        }

        final float[] positions = new float[12];
        final float[] uvs = new float[8];
        final float[] normals = new float[3];
        final Vector3f transformedNormal = pose.transformNormal(polygon.normal, new Vector3f());

        normals[0] = transformedNormal.x();
        normals[1] = transformedNormal.y();
        normals[2] = transformedNormal.z();

        for (int i = 0; i < 4; i++) {
            final ModelPart.Vertex vertex = vertices[i];
            final float modelX = vertex.pos.x() / 16.0F;
            final float modelY = vertex.pos.y() / 16.0F;
            final float modelZ = vertex.pos.z() / 16.0F;
            final Vector3f transformedPos = pose.pose().transformPosition(modelX, modelY, modelZ, new Vector3f());
            final int posIndex = i * 3;
            final int uvIndex = i * 2;

            positions[posIndex] = transformedPos.x();
            positions[posIndex + 1] = transformedPos.y();
            positions[posIndex + 2] = transformedPos.z();

            uvs[uvIndex] = vertex.u;
            uvs[uvIndex + 1] = vertex.v;
        }

        return new GeometryRegistry.QuadTemplate(positions, uvs, normals, normalToDirection(transformedNormal));
    }

    private static String topLevelPart(final String path) {
        if (path == null || path.isEmpty()) {
            return "root";
        }

        String normalized = path;
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }

        if (normalized.isEmpty()) {
            return "root";
        }

        final int firstSeparator = normalized.indexOf('/');
        if (firstSeparator < 0) {
            return normalized;
        }

        final String first = normalized.substring(0, firstSeparator);
        final String remainder = normalized.substring(firstSeparator + 1);
        if (remainder.isEmpty()) {
            return first;
        }

        final int secondSeparator = remainder.indexOf('/');
        final String second = secondSeparator >= 0 ? remainder.substring(0, secondSeparator) : remainder;
        if ((first.equals("main") || first.equals("root")) && !second.isEmpty()) {
            return second;
        }

        return first;
    }

    public static Direction normalToDirection(final Vector3fc normal) {
        for (Direction dir : Direction.values()) {
            if (dir.getStepX() == Math.round(normal.x())
                    && dir.getStepY() == Math.round(normal.y())
                    && dir.getStepZ() == Math.round(normal.z())) {
                return dir;
            }
        }

        final float x = normal.x();
        final float y = normal.y();
        final float z = normal.z();
        final float absX = Math.abs(x);
        final float absY = Math.abs(y);
        final float absZ = Math.abs(z);

        if (absX > absY && absX > absZ) {
            return x > 0 ? Direction.EAST : Direction.WEST;
        }
        if (absY > absZ) {
            return y > 0 ? Direction.UP : Direction.DOWN;
        }
        return z > 0 ? Direction.SOUTH : Direction.NORTH;
    }
}
