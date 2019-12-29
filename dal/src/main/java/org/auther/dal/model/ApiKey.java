package org.auther.dal.model;

import org.immutables.value.Value;

@Value.Immutable
@DOStyle
public interface ApiKey extends AbstractDO {
    String getJti();
    String getKey();
    String getAppId();

    interface Builder extends AbstractDO.Builder {}
}
