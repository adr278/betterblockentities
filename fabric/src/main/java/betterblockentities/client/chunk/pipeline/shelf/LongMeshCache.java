package betterblockentities.client.chunk.pipeline.shelf;

/* java */
import static java.lang.Integer.bitCount;
import static java.util.Arrays.fill;

/**
 * Fixed-size long -> Object cache with linear probing and a simple clock eviction.
 * After construction: zero allocations per get/put.
 */
public final class LongMeshCache<V> {
    private static final long EMPTY = 0L;

    private final Class<V> valueType;
    private final long[] keys;
    private final Object[] values;
    private final byte[] use;
    private int hand;

    public LongMeshCache(int capacityPowerOfTwo, Class<V> valueType) {
        if (bitCount(capacityPowerOfTwo) != 1)
            throw new IllegalArgumentException("capacity must be power of two");

        this.valueType = valueType;
        keys = new long[capacityPowerOfTwo];
        values = new Object[capacityPowerOfTwo];
        use = new byte[capacityPowerOfTwo];
    }

    public V get(long key) {
        key = normalizeKey(key);

        int mask = keys.length - 1;
        int idx = (int) mixToIndex(key) & mask;

        for (int probe = 0; probe < keys.length; probe++) {
            long k = keys[idx];
            if (k == EMPTY) return null;
            if (k == key) {
                use[idx] = 1;
                return valueType.cast(values[idx]);
            }
            idx = (idx + 1) & mask;
        }
        return null;
    }

    public void put(long key, V value) {
        key = normalizeKey(key);

        int mask = keys.length - 1;
        int idx = (int) mixToIndex(key) & mask;

        // Insert/update if we find a free slot or exact match.
        for (int probe = 0; probe < keys.length; probe++) {
            long k = keys[idx];
            if (k == EMPTY || k == key) {
                keys[idx] = key;
                values[idx] = value;
                use[idx] = 1;
                return;
            }
            idx = (idx + 1) & mask;
        }
        // Table full: clock eviction.
        evictAndPut(key, value);
    }

    public void clear() {
        fill(keys, EMPTY);
        fill(values, null);
        fill(use, (byte) 0);
        hand = 0;
    }

    private void evictAndPut(long key, V value) {
        int mask = keys.length - 1;
        for (int step = 0; step < keys.length * 2; step++) {
            int idx = hand & mask;
            hand++;

            if (use[idx] == 0) {
                keys[idx] = key;
                values[idx] = value;
                use[idx] = 1;
                return;
            }
            use[idx] = 0;
        }

        // Fallback: overwrite current hand position.
        int idx = hand & mask;
        keys[idx] = key;
        values[idx] = value;
        use[idx] = 1;
        hand++;
    }

    private static long normalizeKey(long key) {
        return key == EMPTY ? 1L : key;
    }

    private static long mixToIndex(long x) {
        // fast index mix.
        x ^= (x >>> 33);
        x *= 0xff51afd7ed558ccdL;
        x ^= (x >>> 33);
        return x;
    }
}
