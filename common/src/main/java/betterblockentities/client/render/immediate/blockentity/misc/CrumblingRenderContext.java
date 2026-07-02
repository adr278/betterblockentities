package betterblockentities.client.render.immediate.blockentity.misc;

public final class CrumblingRenderContext {
    private static final ThreadLocal<Integer> CRUMBLING_DEPTH = ThreadLocal.withInitial(() -> 0);

    private CrumblingRenderContext() {}

    public static void push() {
        CRUMBLING_DEPTH.set(CRUMBLING_DEPTH.get() + 1);
    }

    public static void pop() {
        final int depth = CRUMBLING_DEPTH.get();
        if (depth <= 1) {
            CRUMBLING_DEPTH.remove();
            return;
        }

        CRUMBLING_DEPTH.set(depth - 1);
    }

    public static boolean isActive() {
        return CRUMBLING_DEPTH.get() > 0;
    }
}
