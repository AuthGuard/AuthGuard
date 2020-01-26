package com.authguard.dal.model;

public interface AbstractDO {
    String getId();
    boolean isDeleted();

    interface Builder {
        Builder id(String id);
        Builder deleted(boolean deleted);
    }
}
