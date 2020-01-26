package com.authguard.service.impl;

import com.authguard.service.PermissionsService;
import com.authguard.service.RolesService;
import com.authguard.service.model.PermissionBO;
import org.jeasy.random.EasyRandom;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PermissionsAggregatorTest {
    private RolesService rolesService;
    private PermissionsService permissionsService;
    private PermissionsAggregator permissionsAggregator;

    private final static EasyRandom RANDOM = new EasyRandom();

    @BeforeAll
    void setup() {
        rolesService = Mockito.mock(RolesService.class);
        permissionsService = Mockito.mock(PermissionsService.class);
        permissionsAggregator = new PermissionsAggregator(rolesService, permissionsService);
    }

    @AfterEach
    void reset() {
        Mockito.reset(rolesService, permissionsService);
    }

    @Test
    void aggregateAccountsPermissions() {
        final PermissionBO standardPermission = RANDOM.nextObject(PermissionBO.class);
        final PermissionBO wildCardPermission = RANDOM.nextObject(PermissionBO.class).withName("*");
        final List<PermissionBO> wildCardGroupPermissions = RANDOM.objects(PermissionBO.class, 5)
                .map(permission -> permission.withGroup(wildCardPermission.getGroup()))
                .collect(Collectors.toList());

        Mockito.when(permissionsService.getAllForGroup(wildCardPermission.getGroup()))
                .thenReturn(wildCardGroupPermissions);

        final List<PermissionBO> expectedPermissions = Stream.concat(Stream.of(standardPermission), wildCardGroupPermissions.stream())
                .collect(Collectors.toList());

        final List<PermissionBO> aggregatedPermissions = permissionsAggregator.aggregate(Collections.emptyList(),
                Arrays.asList(standardPermission, wildCardPermission));

        assertThat(aggregatedPermissions).isEqualTo(expectedPermissions);
    }

    @Test
    void aggregateRolePermissions() {
        final String role = RANDOM.nextObject(String.class);
        final PermissionBO standardPermission = RANDOM.nextObject(PermissionBO.class);
        final PermissionBO wildCardPermission = RANDOM.nextObject(PermissionBO.class).withName("*");
        final List<PermissionBO> wildCardGroupPermissions = RANDOM.objects(PermissionBO.class, 5)
                .map(permission -> permission.withGroup(wildCardPermission.getGroup()))
                .collect(Collectors.toList());

        Mockito.when(rolesService.getPermissionsByName(role)).thenReturn(Arrays.asList(standardPermission, wildCardPermission));

        Mockito.when(permissionsService.getAllForGroup(wildCardPermission.getGroup()))
                .thenReturn(wildCardGroupPermissions);

        final List<PermissionBO> expectedPermissions = Stream.concat(Stream.of(standardPermission), wildCardGroupPermissions.stream())
                .collect(Collectors.toList());

        final List<PermissionBO> aggregatedPermissions = permissionsAggregator.aggregate(Collections.singletonList(role),
                Collections.emptyList());

        assertThat(aggregatedPermissions).isEqualTo(expectedPermissions);
    }
}