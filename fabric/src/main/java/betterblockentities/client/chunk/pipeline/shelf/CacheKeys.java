package betterblockentities.client.chunk.pipeline.shelf;

public final class CacheKeys {
    private CacheKeys() {}

    public static final int NO_TINT = Integer.MIN_VALUE;

    public record StackKey(int itemRawId, int countOrZero, int componentsHash) {}

    public static int stableSeed(StackKey sk) {
        int h = 0x9747b28c;
        h ^= sk.itemRawId();
        h *= 0x85ebca6b;
        h ^= sk.componentsHash();
        h *= 0xc2b2ae35;
        h ^= sk.countOrZero();
        h *= 0x27d4eb2d;
        h ^= (h >>> 16);
        h *= 0x85ebca6b;
        h ^= (h >>> 13);
        h *= 0xc2b2ae35;
        h ^= (h >>> 16);
        return h;
    }

    public static long packSig0(int itemRawId, int componentsHash) {
        return ((long) itemRawId << 32) | (componentsHash & 0xffffffffL);
    }

    public static long packSig1(int countOrZero) {
        return ((long) countOrZero << 32) | 0x9E3779B9L;
    }

    /**
     * Mix two 64-bit signature values into one 64-bit key suitable for primitive hash tables.
     */
    public static long mix64(long a, long b) {
        long x = a ^ (b + 0x9E3779B97F4A7C15L);
        x = (x ^ (x >>> 30)) * 0xBF58476D1CE4E5B9L;
        x = (x ^ (x >>> 27)) * 0x94D049BB133111EBL;
        return x ^ (x >>> 31);
    }
}
