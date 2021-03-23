package com.nexblocks.authguard.jwt;

import com.nexblocks.authguard.service.model.AccountBO;
import com.nexblocks.authguard.service.model.PermissionBO;
import com.nexblocks.authguard.service.model.TokenRestrictionsBO;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class JwtPermissionsMapperTest {

    @Test
    void map() {
        final AccountBO account = AccountBO.builder()
                .permissions(Arrays.asList(
                        PermissionBO.builder()
                                .group("test")
                                .name("read")
                                .build(),
                        PermissionBO.builder()
                                .group("test")
                                .name("write")
                                .build(),
                        PermissionBO.builder()
                                .group("test")
                                .name("nothing")
                                .build()
                ))
                .build();

        final TokenRestrictionsBO restrictions = TokenRestrictionsBO.builder()
                .addPermissions("test:read", "test:nothing")
                .build();

        final String[] expected = new String[] { "test:read", "test:nothing" };
        final String[] actual = JwtPermissionsMapper.map(account, restrictions);

        assertThat(Arrays.asList(actual)).isEqualTo(Arrays.asList(expected));
    }

    @Test
    void mapNoPermissionRestrictions() {
        final AccountBO account = AccountBO.builder()
                .permissions(Arrays.asList(
                        PermissionBO.builder()
                                .group("test")
                                .name("read")
                                .build(),
                        PermissionBO.builder()
                                .group("test")
                                .name("write")
                                .build(),
                        PermissionBO.builder()
                                .group("test")
                                .name("nothing")
                                .build()
                ))
                .build();

        final TokenRestrictionsBO restrictions = TokenRestrictionsBO.builder().build();

        final String[] expected = new String[] { "test:read", "test:write", "test:nothing" };
        final String[] actual = JwtPermissionsMapper.map(account, restrictions);

        assertThat(Arrays.asList(actual)).isEqualTo(Arrays.asList(expected));
    }

    @Test
    void mapNoRestrictions() {
        final AccountBO account = AccountBO.builder()
                .permissions(Arrays.asList(
                        PermissionBO.builder()
                                .group("test")
                                .name("read")
                                .build(),
                        PermissionBO.builder()
                                .group("test")
                                .name("write")
                                .build(),
                        PermissionBO.builder()
                                .group("test")
                                .name("nothing")
                                .build()
                ))
                .build();

        final String[] expected = new String[] { "test:read", "test:write", "test:nothing" };
        final String[] actual = JwtPermissionsMapper.map(account, null);

        assertThat(Arrays.asList(actual)).isEqualTo(Arrays.asList(expected));
    }
}