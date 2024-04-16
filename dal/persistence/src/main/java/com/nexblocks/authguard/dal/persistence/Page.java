package com.nexblocks.authguard.dal.persistence;

import java.util.Objects;

public class Page<T> {
    private final T cursor;
    private final int count;

    private Page(T cursor, int count) {
        this.cursor = cursor;
        this.count = count;
    }

    public static <T> Page<T> of(T cursor, int count, T defaultCursor) {
        return new Page<T>(cursor != null ? cursor : defaultCursor, count);
    }

    public T getCursor() {
        return cursor;
    }

    public int getCount() {
        return count;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Page<?> page = (Page<?>) o;
        return cursor == page.cursor && count == page.count;
    }

    @Override
    public int hashCode() {
        return Objects.hash(cursor, count);
    }
}
