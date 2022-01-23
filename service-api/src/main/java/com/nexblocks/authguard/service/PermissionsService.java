package com.nexblocks.authguard.service;

import com.nexblocks.authguard.service.model.PermissionBO;

import java.util.List;

public interface PermissionsService extends CrudService<PermissionBO> {
    List<PermissionBO> validate(List<PermissionBO> permissions, String domain);
    List<PermissionBO> getAll(String domain);
    List<PermissionBO> getAllForGroup(String group, String domain);
}
