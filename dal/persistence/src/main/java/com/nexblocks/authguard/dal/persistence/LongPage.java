package com.nexblocks.authguard.dal.persistence;

public class LongPage {
    public static Page<Long> of(Long cursor, int count) {
        return Page.of(cursor, count, 0L);
    }
}
