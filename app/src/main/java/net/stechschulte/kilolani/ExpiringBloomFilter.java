package net.stechschulte.kilolani;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by john on 4/29/17.
 */

public class ExpiringBloomFilter {
    private int n_bits;
    private int n_longs;
    private int n_hashes;
    private long expire_time;
    private HashMap<Integer, Long> hashTable;

    public ExpiringBloomFilter(int bits, int hashes, long expire_time) {
        n_bits = bits;
        n_longs = (bits + 63)/64;
        n_hashes = hashes;
        this.expire_time = expire_time;
        hashTable = new HashMap<>(n_bits);
    }

    public long[] getFilter() {
        long[] r = new long[n_longs];
        long exp_threshold = System.currentTimeMillis() - expire_time;
        for (Map.Entry<Integer, Long> bit : hashTable.entrySet()) {
            if (bit.getValue() < exp_threshold) {
                hashTable.remove(bit.getKey());
            } else {
                long k = bit.getKey();
                r[(int)(k/64)] |= 1<<(k%64);
            }
        }
        return r;
    }

    public void insert(String item, long time) {
        for (int i=0; i<n_hashes; i++) {
            hashTable.put(String.format("%s%d", item, i).hashCode()%n_bits, time);
        }

        if (hashTable.size() > 0.5*n_bits) {
            expire();
        }
    }

    public static boolean mightContain(long[] table, int n_bits, int n_hashes, String item) {
        boolean r = true;
        for (int i=0; i<n_hashes; i++) {
            int h = String.format("%s%d", item, i).hashCode()%n_bits;
            r = r && ((table[h/64] & (1<<(h%64)))>0);
        }
        return r;
    }

    public boolean mightContain(String item) {
        boolean r = true;
        for (int i=0; i<n_hashes; i++) {
            r = r && hashTable.containsKey(String.format("%s%d", item, i).hashCode()%n_bits);
        }
        return r;
    }

    public void insert(String item) {
        insert(item, System.currentTimeMillis());
    }

    public void expire() {
        getFilter();
    }
}
