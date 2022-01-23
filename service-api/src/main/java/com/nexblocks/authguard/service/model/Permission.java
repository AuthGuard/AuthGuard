package com.nexblocks.authguard.service.model;

import org.immutables.value.Value;

@Value.Immutable
@BOStyle
public interface Permission extends Entity {
    String getGroup();
    String getName();
    String getDomain();

    /**
     * Checks whether a permission is a wild card or not.
     * A wild card permission is one which includes all
     * permissions in a certain group. For example, a
     * wild card permission with group 'account' means that
     * it includes all permissions under group 'account'.
     */
    default boolean isWildCard() {
        return getName().equals("*");
    }

    default String getFullName() {
        return getGroup() + ":" + getName();
    }
}
