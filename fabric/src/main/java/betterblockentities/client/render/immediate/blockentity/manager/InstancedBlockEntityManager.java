package betterblockentities.client.render.immediate.blockentity.manager;

/* local */
import betterblockentities.render.AltRenderers;
import betterblockentities.client.BBE;
import betterblockentities.client.chunk.section.SectionUpdateDispatcher;
import betterblockentities.client.gui.config.BBEConfig;
import betterblockentities.client.gui.config.ConfigCache;
import betterblockentities.client.gui.option.EnumTypes;
import betterblockentities.client.render.immediate.blockentity.misc.RenderingMode;
import betterblockentities.client.render.immediate.blockentity.extentions.BlockEntityExt;
import betterblockentities.client.render.immediate.util.BlockVisibilityChecker;
import betterblockentities.client.tasks.ManagerTasks;

/* minecraft */
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.*;

public final class InstancedBlockEntityManager {
    private enum Phase {
        IDLE,               //manager is inactive. nothing scheduled or required
        IMMEDIATE_ACTIVE,   //blockEntity must currently render using the BER path (animating, duration task, visible under SMART scheduler)
        WAITING_TERRAIN     //we requested a terrain section rebuild and are waiting for the fence callback
    }

    /* bound context */
    private final BlockEntity blockEntity;
    private final BlockEntityExt ext;
    private final BlockPos pos;

    /* runtime state */
    private boolean animating;
    private boolean durationTask;
    private float durationTaskStart;
    private float duration;

    /* internal lifecycle phase of this manager */
    private Phase phase = Phase.IDLE;

    public InstancedBlockEntityManager(BlockEntity blockEntity) {
        this.blockEntity = blockEntity;
        this.ext = (BlockEntityExt)blockEntity;
        this.pos = blockEntity.getBlockPos();
    }

    public BlockEntity getBlockEntity() { return blockEntity; }

    public boolean isIdle() { return phase == Phase.IDLE; }

    public boolean isAnimating() { return animating; }

    /**
     * Internal setter for animation state. If animation starts while waiting for terrain rebuild,
     * we immediately return to IMMEDIATE rendering.
     */
    private void setAnimating(boolean animating) {
        this.animating = animating;
        if (animating && phase == Phase.WAITING_TERRAIN) {
            enterImmediate();
        }
    }

    /**
     * Starts or updates a temporary duration-based animation. (e.g., Decorated Pots)
     */
    public void setDurationTask(boolean enabled, float start, float duration) {
        this.durationTask = enabled;
        this.durationTaskStart = start;
        this.duration = duration;

        if (enabled && phase == Phase.WAITING_TERRAIN) {
            enterImmediate();
        }
    }

    /**
     * validate this manager, in-case said block entity got removed or is invalid
     */
    public boolean isValid() {
        return !(blockEntity == null || blockEntity.isRemoved());
    }

    /**
     * force kill this manager, reset state
     */
    public void forceKill() {
        this.animating = false;
        this.durationTaskStart = 0;
        this.duration = 0;
        this.phase = Phase.IDLE;
    }

    /**
     * Main scheduler entry point. This is what runs for each scheduled manager
     * Returns:
     *   PROCESSING -> keep scheduled
     *   FINISHED   -> safe to remove from queue
     */
    public int run(float partialTicks) {
        if (!BBEConfig.OptEnabledTable.ENABLED[ext.bbe$getOptKind() & 0xFF] ||
            AltRenderers.hasRendererOverride(blockEntity.getType()))
        {
            phase = Phase.IDLE;
            return ManagerTasks.FINISHED;
        }

        switch (phase) {
            case IDLE -> {
                if (shouldBeImmediate(partialTicks)) {
                    enterImmediate();
                    return ManagerTasks.PROCESSING;
                }
                return ManagerTasks.FINISHED;
            }

            case IMMEDIATE_ACTIVE -> {
                /* stay in IMMEDIATE while conditions remain true */
                if (shouldBeImmediate(partialTicks)) {
                    return ManagerTasks.PROCESSING;
                }
                requestTerrainFence(partialTicks);
                return ManagerTasks.PROCESSING;
            }

            case WAITING_TERRAIN -> {
                /* we do not spin here every tick. the rebuild fence callback will re-schedule us if needed */
                return ManagerTasks.FINISHED;
            }
        }

        return ManagerTasks.FINISHED;
    }

    /**
     * Called from block entity animation tickers each tick to update animation state
     */
    public void tick(boolean animState, boolean animOption) {
        if (!animOption) {
            this.setAnimating(false);
            return;
        }

        boolean old = this.isAnimating();
        this.setAnimating(animState);

        /* edge-trigger scheduling (only wake when state changes) */
        if (old != animState) {
            ManagerTasks.schedule(this);
        }
    }

    /**
     * Called by event-driven animations (e.g., Decorated Pots)
     */
    public void trigger(float start, float duration, boolean animOption) {
        if (!animOption) {
            this.setAnimating(false);
            return;
        }
        this.setDurationTask(true, start, duration);
        ManagerTasks.schedule(this);
    }

    /**
     * Determines whether the BE must remain in IMMEDIATE mode.
     */
    private boolean shouldBeImmediate(float partialTicks) {
        if (animating) return true;
        if (durationTask && isDurationStillRunning(partialTicks)) return true;
        if (isSmartSchedulerEnabled() && isVisibleInFov()) return true;
        return false;
    }

    /**
     * Checks duration task expiration
     */
    private boolean isDurationStillRunning(float partialTicks) {
        if (blockEntity.getLevel() == null) return false;
        float now = blockEntity.getLevel().getGameTime();

        float duration = ((now - durationTaskStart) + partialTicks) / this.duration;

        return duration >= 0.0F && duration <= 1.0F;
    }

    /**
     * Transitions BE into IMMEDIATE rendering. Forces terrain section rebuild so geometry is removed from terrain mesh.
     */
    private void enterImmediate() {
        phase = Phase.IMMEDIATE_ACTIVE;
        ext.bbe$setTerrainMeshReady(false);

        if (ext.bbe$getRenderingMode() != RenderingMode.IMMEDIATE) {
            ext.bbe$setRenderingMode(RenderingMode.IMMEDIATE);
            SectionUpdateDispatcher.queueRebuildAtBlockPos(pos);
        }
    }

    /**
     * Requests chunk rebuild and waits for upload fence before allowing BER cancellation.
     */
    private void requestTerrainFence(float partialTicks) {
        phase = Phase.WAITING_TERRAIN;

        if (ext.bbe$getRenderingMode() != RenderingMode.TERRAIN) {
            ext.bbe$setRenderingMode(RenderingMode.TERRAIN);
        }

        ext.bbe$setTerrainMeshReady(false);

        SectionUpdateDispatcher.queueRebuildAtBlockPos(pos, () -> {
            /* when fence fires, we must potentially resume immediately */
            if (!BBEConfig.OptEnabledTable.ENABLED[ext.bbe$getOptKind() & 0xFF]) {
                ext.bbe$setTerrainMeshReady(true);
                phase = Phase.IDLE;
                ManagerTasks.clearActive(this);
                return;
            }

            /* animation resumed during rebuild */
            if (shouldBeImmediate(partialTicks)) {
                enterImmediate();
                ManagerTasks.schedule(this);
                return;
            }

            /* terrain section finished rebuilding, switch to TERRAIN rendering */
            ext.bbe$setTerrainMeshReady(true);
            phase = Phase.IDLE;
            ManagerTasks.clearActive(this);
        });
    }

    private boolean isSmartSchedulerEnabled() {
        return ConfigCache.updateType == EnumTypes.UpdateSchedulerType.SMART.ordinal();
    }

    private boolean isVisibleInFov() {
        return BlockVisibilityChecker.isBlockInFOVAndVisible(BBE.GlobalScope.frustum, blockEntity) == BlockVisibilityChecker.Visibility.VISIBLE;
    }

    public static final class OptKind {
        private OptKind() {}

        public static final byte NONE   = 0;
        public static final byte CHEST  = 1;
        public static final byte SIGN   = 2;
        public static final byte SHULKER= 3;
        public static final byte POT    = 4;
        public static final byte BANNER = 5;
        public static final byte BELL   = 6;
        public static final byte CGS    = 7;
        public static final byte SHELF  = 8;
        public static final byte CAMPFIRE  = 9;
    }
}