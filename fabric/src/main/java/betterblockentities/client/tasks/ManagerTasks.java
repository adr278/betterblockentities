package betterblockentities.client.tasks;

import betterblockentities.client.render.immediate.blockentity.manager.InstancedBlockEntityManager;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;

/**
 * Manager work queue "executor" runs each enqueued manager until the manager instance returns FINISHED
 * Each entry is submitted via {@link betterblockentities.client.tasks.ManagerTasks -> schedule } from each
 * block entities animation "trigger"
 */
public final class ManagerTasks {
    private ManagerTasks() {}

    public static final Queue<Long> WORK_QUEUE = new ArrayDeque<>();
    private static final Map<Long, Entry> ACTIVE_MANAGERS = new HashMap<>();

    public static final int FINISHED = 0;
    public static final int PROCESSING = 1;

    public static void schedule(InstancedBlockEntityManager mgr) {
        long key = getBlockPosKey(mgr);
        Entry entry = ACTIVE_MANAGERS.computeIfAbsent(key, ignored -> new Entry());

        entry.manager = mgr;
        if (!entry.queued) {
            entry.queued = true;
            WORK_QUEUE.add(key);
        }
    }

    public static void process(float partialTicks) {
        int budget = 256;
        while (budget-- > 0) {
            Long key = WORK_QUEUE.poll();
            if (key == null) break;

            Entry entry = ACTIVE_MANAGERS.get(key);
            if (entry == null) {
                continue;
            }

            entry.queued = false;

            InstancedBlockEntityManager mgr = entry.manager;
            int state = mgr.run(partialTicks);
            if (state == PROCESSING) {
                if (mgr.isValid()) {
                    schedule(mgr);
                } else {
                    mgr.forceKill();
                    clearActive(mgr);
                }
            } else if (mgr.isIdle()) {
                clearActive(mgr);
            }
        }
    }

    public static void clearActive(InstancedBlockEntityManager mgr) {
        long key = getBlockPosKey(mgr);
        Entry entry = ACTIVE_MANAGERS.get(key);
        if (entry != null && entry.manager == mgr) {
            ACTIVE_MANAGERS.remove(key);
        }
    }

    private static long getBlockPosKey(InstancedBlockEntityManager mgr) {
        BlockEntity blockEntity = mgr.getBlockEntity();
        return blockEntity.getBlockPos().asLong();
    }

    private static final class Entry {
        private InstancedBlockEntityManager manager;
        private boolean queued;
    }
}
