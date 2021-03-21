package com.nexblocks.authguard.jwt;

import com.nexblocks.authguard.service.model.AccountBO;
import com.nexblocks.authguard.service.model.PermissionBO;
import com.nexblocks.authguard.service.model.TokenRestrictionsBO;

import java.util.stream.Stream;

public class JwtPermissionsMapper {

    private JwtPermissionsMapper() {}

    static String[] map(final AccountBO account, final TokenRestrictionsBO restrictions) {
        return mapToStream(account, restrictions).toArray(String[]::new);
    }

    private static Stream<String> mapToStream(final AccountBO account, final TokenRestrictionsBO restrictions) {
        if (restrictions == null || restrictions.getPermissions().isEmpty()) {
            return account.getPermissions().stream()
                    .map(JwtPermissionsMapper::permissionToString);
        }

        return account.getPermissions().stream()
                .map(JwtPermissionsMapper::permissionToString)
                .filter(restrictions.getPermissions()::contains);
    }

    private static String permissionToString(final PermissionBO permission) {
        return permission.getGroup() + ":" + permission.getName();
    }
}
