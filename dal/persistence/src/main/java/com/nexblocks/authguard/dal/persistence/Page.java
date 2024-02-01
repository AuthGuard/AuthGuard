package com.nexblocks.authguard.dal.persistence;

import java.util.Objects;

public class Page {
    private final long cursor;
    private final int count;

    private Page(long cursor, int count) {
        this.cursor = cursor;
        this.count = count;
    }

    public static Page of(Long cursor, int count) {
        return new Page(cursor != null ? cursor : 0, count);
    }

    public long getCursor() {
        return cursor;
    }

    public int getCount() {
        return count;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Page page = (Page) o;
        return cursor == page.cursor && count == page.count;
    }

    @Override
    public int hashCode() {
        return Objects.hash(cursor, count);
    }
}
