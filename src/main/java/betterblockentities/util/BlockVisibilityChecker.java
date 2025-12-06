package betterblockentities.util;

/* minecraft */


/* java/misc */
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BannerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BlockVisibilityChecker {
    /* cache grids so they are not regenerated every call */
    private static final Map<Integer, Vec3[]> GRID_CACHE = new HashMap<>();

    public static boolean isBlockInFOVAndVisible(Frustum frustum, BlockEntity blockEntity) {
        /* check if we have a screen open (we count this as "not" visible) */
        Screen curScreen = Minecraft.getInstance().screen;
        if (curScreen != null && !(curScreen instanceof ChatScreen))
            return false;

        Entity player = Minecraft.getInstance().getCameraEntity();
        if (player == null) return false;

        Vec3 eyePos = player.getEyePosition(1.0f);
        BlockPos pos = blockEntity.getBlockPos();
        Level world = blockEntity.getLevel();

        /* setup box to use for frustum and los check */
        AABB box = setupBox(blockEntity, pos);

        /* distance check max 20 blocks */
        double maxDistanceSq = 20 * 20;
        if (eyePos.distanceToSqr(box.getCenter()) > maxDistanceSq)
            return false;

        /* frustum check */
        if (!isBlockInViewFrustum(frustum, box))
            return false;

        /* do we have LOS? */
        return hasLOS(world, eyePos, box);
    }

    /* is the generated box inside the frustum? */
    private static boolean isBlockInViewFrustum(Frustum frustum, AABB box) {
        return frustum != null && frustum.isVisible(box);
    }

    /*
        multipoint voxel raycast - this implementation is a bit janky and is highly customized for our needs
        there is probably a much cheaper way to check LOS, im not to read up on this kind of stuff,
        voxel raytracing should be cheaper than RaycastContext anyway, this works fine for our purposes
    */
    private static boolean hasLOS(Level world, Vec3 eye, AABB box) {
        double distance = eye.distanceTo(box.getCenter());

        /* dynamic resolution: decrease by 1 every 4 blocks */
        int resolution = Math.max(2, 5 - (int)(distance / 4));

        Vec3[] samplePoints = GRID_CACHE.computeIfAbsent(resolution, BlockVisibilityChecker::generateFaceGrid);

        /* check against generated points */
        for (Vec3 offset : samplePoints) {
            Vec3 target = new Vec3(
                    box.minX + offset.x * box.getXsize(), //double check
                    box.minY + offset.y * box.getYsize(), //double check
                    box.minZ + offset.z * box.getZsize() //double check
            );
            if (raycastVoxel(world, eye, target))
                return true; // at least 1 point is visible
        }
        return false;
    }

    /* voxel raycast from eye to target point */
    private static boolean raycastVoxel(Level world, Vec3 eye, Vec3 target) {
        Vec3 ray = target.subtract(eye);
        double maxDistSq = ray.lengthSqr();

        /* step size: small for near rays, bigger for far rays */
        double step = (ray.length() < 10) ? 0.25 : 0.35;

        Vec3 dir = ray.normalize().scale(step);
        Vec3 cur = eye.add(dir);

        while (eye.distanceToSqr(cur) < maxDistSq) {

            BlockPos pos = BlockPos.containing(cur);
            BlockState state = world.getBlockState(pos);
            Block block = state.getBlock();

            /* custom declaration of what to skip */
            if (state.propagatesSkylightDown() && !state.canOcclude() ||
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

            if (!shape.isEmpty() && shape.clip(cur, target, pos) != null)
                return false; // blocked

            /* check fluid only if present */
            VoxelShape fluid = state.getFluidState().getShape(world, pos);
            if (!fluid.isEmpty() && fluid.clip(cur, target, pos) != null)
                return false; // blocked

            cur = cur.add(dir);
        }
        return true;
    }

    private static Vec3[] generateFaceGrid(int resolution) {
        List<Vec3> list = new ArrayList<>();
        double step = 1.0 / (resolution - 1);

        for (int y = 0; y < resolution; y++) {
            for (int x = 0; x < resolution; x++) {
                double u = x * step; // horizontal
                double v = y * step; // vertical

                list.add(new Vec3(0, v, u));
                list.add(new Vec3(1, v, u));
                list.add(new Vec3(u, v, 0));
                list.add(new Vec3(u, v, 1));
                list.add(new Vec3(u, 0, v));
                list.add(new Vec3(u, 1, v));
            }
        }
        return list.toArray(new Vec3[0]);
    }

    /*
        setup bounding box, we could probably skip the function above
        and just expand the existing box instead of using box union
        which should be slightly more efficient. guess this is a TODO
    */
    private static AABB setupBox(BlockEntity be, BlockPos pos) {
        if (be instanceof BannerBlockEntity)
            return new AABB(pos).inflate(0, 1, 0);

        if (!(be instanceof ChestBlockEntity))
            return new AABB(pos);

        BlockPos other = getOtherChestHalf(be.getLevel(), pos);
        return (other == null) ?
                new AABB(pos) :
                new AABB(pos).minmax(new AABB(other));
    }

    private static BlockPos getOtherChestHalf(Level world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        if (!(state.getBlock() instanceof ChestBlock))
            return null;

        ChestType type = state.getValue(ChestBlock.TYPE);
        if (type == ChestType.SINGLE)
            return null;

        Direction facing = state.getValue(ChestBlock.FACING);

        Direction side = (type == ChestType.LEFT)
                ? facing.getClockWise()
                : facing.getCounterClockWise();

        BlockPos otherPos = pos.relative(side);

        if (world.getBlockState(otherPos).getBlock() instanceof ChestBlock)
            return otherPos;

        return null;
    }
}
