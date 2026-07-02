package com.nexblocks.authguard.testdistribution.bootstrap;

import com.nexblocks.authguard.api.dto.requests.CreateAccountRequestDTO;
import com.nexblocks.authguard.api.dto.requests.CreateAppRequestDTO;
import com.nexblocks.authguard.api.dto.requests.CreateClientRequestDTO;
import com.nexblocks.authguard.api.dto.entities.RoleDTO;
import com.nexblocks.authguard.api.dto.entities.PermissionDTO;

import java.util.List;

public class BootstrapData {
    private List<RoleDTO> roles;
    private List<PermissionDTO> permissions;
    private List<CreateAccountRequestDTO> accounts;
    private List<CreateAppRequestDTO> applications;
    private List<CreateClientRequestDTO> clients;

    public List<RoleDTO> getRoles() {
        return roles;
    }

    public void setRoles(List<RoleDTO> roles) {
        this.roles = roles;
    }

    public List<PermissionDTO> getPermissions() {
        return permissions;
    }

    public void setPermissions(List<PermissionDTO> permissions) {
        this.permissions = permissions;
    }

    public List<CreateAccountRequestDTO> getAccounts() {
        return accounts;
    }

    public void setAccounts(List<CreateAccountRequestDTO> accounts) {
        this.accounts = accounts;
    }

    public List<CreateAppRequestDTO> getApplications() {
        return applications;
    }

    public void setApplications(List<CreateAppRequestDTO> applications) {
        this.applications = applications;
    }

    public List<CreateClientRequestDTO> getClients() {
        return clients;
    }

    public void setClients(List<CreateClientRequestDTO> clients) {
        this.clients = clients;
    }
}
