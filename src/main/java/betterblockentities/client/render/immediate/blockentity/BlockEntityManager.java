package betterblockentities.client.render.immediate.blockentity;

/* local */
import betterblockentities.client.BBE;
import betterblockentities.client.gui.config.ConfigCache;
import betterblockentities.client.gui.option.EnumTypes;
import betterblockentities.client.render.immediate.util.BlockVisibilityChecker;
import betterblockentities.client.chunk.SectionUpdateDispatcher;

/* minecraft */
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.phys.Vec3;

/* java/misc */
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import java.util.List;

/**
 * This manager handles each BlockEntity present in {@link "net.caffeinemc.mods.sodium.client.render.SodiumWorldRenderer#extractBlockEntity"}
 * We take data like animation state, BlockEntity type, block visibility and overall state changes into account to decide whether to extract
 * this BlockEntity or not. The end Node of this manager sets {@link betterblockentities.client.render.immediate.blockentity.BlockEntityExt}
 * -> getRemoveChunkVariant which is unique to each BlockEntity and decides if we should mesh its geometry or not and triggers a section
 * remesh and stop the immediate rendering pipeline from rendering this BlockEntity
 */
public class BlockEntityManager {
    public static ReferenceOpenHashSet<Class<? extends BlockEntity>> SUPPORTED_TYPES = new ReferenceOpenHashSet<>();
    public static final ReferenceOpenHashSet<Class<? extends Block>> SUPPORTED_BLOCKS = new ReferenceOpenHashSet<>(
            List.of(
                    ChestBlock.class, EnderChestBlock.class, TrappedChestBlock.class,
                    CopperChestBlock.class, WeatheringCopperChestBlock.class,
                    ShulkerBoxBlock.class, BellBlock.class, DecoratedPotBlock.class,
                    BedBlock.class, CeilingHangingSignBlock.class, WallHangingSignBlock.class,
                    StandingSignBlock.class, WallSignBlock.class,
                    BannerBlock.class, WallBannerBlock.class, CopperGolemStatueBlock.class,
                    WeatheringCopperGolemStatueBlock.class
            )
    );

    public static boolean isSupportedBlock(Block block) {
        return block != null && SUPPORTED_BLOCKS.contains(block.getClass());
    }

    public static boolean isSupportedEntity(BlockEntity blockEntity) {
        return blockEntity != null && SUPPORTED_TYPES.contains(blockEntity.getClass());
    }

    public static boolean isSignEntity(BlockEntity blockEntity) {
        return blockEntity != null &&
                SignBlockEntity.class == blockEntity.getClass() ||
                HangingSignBlockEntity.class == blockEntity.getClass();
    }

    private static boolean isAnimating(BlockEntity blockEntity) {
        if (ConfigCache.chestAnims && blockEntity instanceof LidBlockEntity lid)
            return lid.getOpenNess(0.5f) > 0f;
        if (ConfigCache.shulkerAnims && blockEntity instanceof ShulkerBoxBlockEntity shulker)
            return shulker.getProgress(0.5f) > 0f;
        if (ConfigCache.bellAnims && blockEntity instanceof BellBlockEntity bell)
            return bell.shaking;
        if (ConfigCache.potAnims && blockEntity instanceof DecoratedPotBlockEntity pot && pot.lastWobbleStyle != null) {
            long now = blockEntity.getLevel().getGameTime();
            return now - pot.wobbleStartedAtTick < pot.lastWobbleStyle.duration;
        }
        if (ConfigCache.signText && blockEntity instanceof SignBlockEntity) {
                return shouldRenderSignText((SignBlockEntity)blockEntity);
        }

        /* banners and beds are handled here */
        return false;
    }

    /* quick check for text before we reach the ber (where we cull the text) */
    private static boolean shouldRenderSignText(SignBlockEntity blockEntity) {
        if (blockEntity.getBackText() == null && blockEntity.getFrontText() == null)
            return false;

        Entity entity = Minecraft.getInstance().getCameraEntity();
        return entity.distanceToSqr(Vec3.atCenterOf(blockEntity.getBlockPos())) < ConfigCache.signTextRenderDistance * ConfigCache.signTextRenderDistance;
    }

    public static boolean shouldRender(BlockEntity blockEntity) {
        if (!ConfigCache.masterOptimize) return true;

        /* are we a supported BE and are the config options enabled */
        if (!isSupportedEntity(blockEntity)) return true;

        /* did we just receive a block event, if not don't render with BER */
        BlockEntityExt inst = (BlockEntityExt) blockEntity;
        if (!inst.getJustReceivedUpdate() && !isSignEntity(blockEntity)) {
            return false;
        }

        /* animation logic (static and animating) */
        return isAnimating(blockEntity) ? handleAnimating(blockEntity, inst) : handleStatic(blockEntity, inst);
    }

    private static boolean handleAnimating(BlockEntity blockEntity, BlockEntityExt inst) {
        var pos = blockEntity.getBlockPos().asLong();

        /* ignore signs as we render the text with its BER  */
        if (!(blockEntity instanceof SignBlockEntity)) {
            /* add to anim map if an entry doesn't exist */
            if (BlockEntityTracker.animMap.add(pos)) {
                inst.setRemoveChunkVariant(true);
                SectionUpdateDispatcher.queueRebuildAtBlockPos(blockEntity.getLevel(), pos);
            }
        }
        return true;
    }

    private static boolean handleStatic(BlockEntity blockEntity, BlockEntityExt inst) {
        var pos = blockEntity.getBlockPos().asLong();

        if (ConfigCache.updateType == EnumTypes.UpdateSchedulerType.SMART.ordinal()) {
            if (!BlockEntityTracker.animMap.contains(pos)) return false;

            /* captured frustum */
            var frustum = BBE.curFrustum;

            /* check sanity (visible or not) */
            if (BlockVisibilityChecker.isBlockInFOVAndVisible(frustum, blockEntity))
                return true;
        }

        if (BlockEntityTracker.animMap.remove(pos)) {
            inst.setRemoveChunkVariant(false);
            SectionUpdateDispatcher.queueRebuildAtBlockPos(blockEntity.getLevel(), pos);
            BlockEntityTracker.extraRenderPasses.put(pos, ConfigCache.renderpasses);
        }

        int passes = BlockEntityTracker.extraRenderPasses.compute(pos, (p, v) -> {
            if (v == null) return null;
            if (v > 1) return v - 1;
            inst.setJustReceivedUpdate(false);
            return null;
        });
        return passes != 0;
    }
}
