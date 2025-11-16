package betterblockentities.util;

/* minecraft */
import net.minecraft.block.*;
import net.minecraft.block.entity.BannerBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.enums.ChestType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Frustum;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.*;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.World;

/* java/misc */
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BlockVisibilityChecker {
    /* cache grids so they are not regenerated every call */
    private static final Map<Integer, Vec3d[]> GRID_CACHE = new HashMap<>();

    public static boolean isBlockInFOVAndVisible(Frustum frustum, BlockEntity blockEntity) {
        Entity player = MinecraftClient.getInstance().getCameraEntity();
        if (player == null) return false;

        Vec3d eyePos = player.getCameraPosVec(1.0f);
        BlockPos pos = blockEntity.getPos();
        World world = blockEntity.getWorld();

        Box box = setupBox(blockEntity, pos);

        /* distance check max 20 blocks */
        double maxDistanceSq = 20 * 20;
        if (eyePos.squaredDistanceTo(box.getCenter()) > maxDistanceSq)
            return false;

        /* frustum check */
        if (!isBlockInViewFrustum(frustum, box))
            return false;

        /* do we have LOS? */
        return hasLOS(world, eyePos, box);
    }

    /* is the generated box inside the frustum? */
    private static boolean isBlockInViewFrustum(Frustum frustum, Box box) {
        return frustum != null && frustum.isVisible(box);
    }

    /*
        multipoint voxel raycast - this implementation is a bit janky and is highly customized for our needs
        there is probably a much cheaper way to check LOS, im not to read up on this kind of stuff,
        voxel raytracing should be cheaper than RaycastContext anyway, this works fine for our purposes
    */
    private static boolean hasLOS(World world, Vec3d eye, Box box) {
        double distance = eye.distanceTo(box.getCenter());

        /* dynamic resolution: decrease by 1 every 4 blocks */
        int resolution = Math.max(2, 5 - (int)(distance / 4));

        Vec3d[] samplePoints = GRID_CACHE.computeIfAbsent(resolution, BlockVisibilityChecker::generateFaceGrid);

        /* check against generated points */
        for (Vec3d offset : samplePoints) {
            Vec3d target = new Vec3d(
                    box.minX + offset.x * box.getLengthX(),
                    box.minY + offset.y * box.getLengthY(),
                    box.minZ + offset.z * box.getLengthZ()
            );
            if (raycastVoxel(world, eye, target))
                return true; // at least 1 point is visible
        }
        return false;
    }

    /* voxel raycast from eye to target point */
    private static boolean raycastVoxel(World world, Vec3d eye, Vec3d target) {
        Vec3d ray = target.subtract(eye);
        double maxDistSq = ray.lengthSquared();

        /* step size: small for near rays, bigger for far rays */
        double step = (ray.length() < 10) ? 0.25 : 0.35;

        Vec3d dir = ray.normalize().multiply(step);
        Vec3d cur = eye.add(dir);

        while (eye.squaredDistanceTo(cur) < maxDistSq) {

            BlockPos pos = BlockPos.ofFloored(cur);
            BlockState state = world.getBlockState(pos);
            Block block = state.getBlock();

            /* custom declaration of what to skip */
            if (state.isTransparent() && !state.isOpaque() ||
                    block instanceof FenceBlock ||
                    block instanceof FenceGateBlock ||
                    block instanceof TintedGlassBlock)
            {
                cur = cur.add(dir);
                continue;
            }

            /*
                NOTE: doing separate raycast for fluids and blocks should be
                more efficient than combining both shapes into one via VoxelShapes.union()
                or???? might be worth profiling
             */

            /* try block collision first */
            VoxelShape shape = state.getCollisionShape(world, pos);

            if (!shape.isEmpty() && shape.raycast(cur, target, pos) != null)
                return false; // blocked

            /* check fluid only if present */
            VoxelShape fluid = state.getFluidState().getShape(world, pos);
            if (!fluid.isEmpty() && fluid.raycast(cur, target, pos) != null)
                return false; // blocked

            cur = cur.add(dir);
        }
        return true;
    }

    private static Vec3d[] generateFaceGrid(int resolution) {
        List<Vec3d> list = new ArrayList<>();
        double step = 1.0 / (resolution - 1);

        for (int y = 0; y < resolution; y++) {
            for (int x = 0; x < resolution; x++) {
                double u = x * step; // horizontal
                double v = y * step; // vertical

                list.add(new Vec3d(0, v, u));
                list.add(new Vec3d(1, v, u));
                list.add(new Vec3d(u, v, 0));
                list.add(new Vec3d(u, v, 1));
                list.add(new Vec3d(u, 0, v));
                list.add(new Vec3d(u, 1, v));
            }
        }
        return list.toArray(new Vec3d[0]);
    }

    /*
        setup bounding box, we could probably skip the function above
        and just expand the existing box instead of using box union
        which should be slightly more efficient. guess this is a TODO
    */
    private static Box setupBox(BlockEntity be, BlockPos pos) {
        if (be instanceof BannerBlockEntity)
            return new Box(pos).expand(0, 1, 0);

        if (!(be instanceof ChestBlockEntity))
            return new Box(pos);

        BlockPos other = getOtherChestHalf(be.getWorld(), pos);
        return (other == null) ?
                new Box(pos) :
                new Box(pos).union(new Box(other));
    }

    private static BlockPos getOtherChestHalf(World world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        if (!(state.getBlock() instanceof ChestBlock))
            return null;

        ChestType type = state.get(ChestBlock.CHEST_TYPE);
        if (type == ChestType.SINGLE)
            return null;

        Direction facing = state.get(ChestBlock.FACING);

        Direction side = (type == ChestType.LEFT)
                ? facing.rotateYClockwise()
                : facing.rotateYCounterclockwise();

        BlockPos otherPos = pos.offset(side);

        if (world.getBlockState(otherPos).getBlock() instanceof ChestBlock)
            return otherPos;

        return null;
    }
}
