package betterblockentities.client.render.immediate.blockentity.misc;

public final class CrumblingRenderContext {
    private static int crumblingDepth;

    public static void push() {
        crumblingDepth++;
    }

    public static void pop() {
        if (crumblingDepth > 0) {
            crumblingDepth--;
        }
    }

    public static boolean isActive() {
        return crumblingDepth > 0;
    }
}
