package org.auther.rest.routes;

import org.auther.rest.dto.*;
import org.auther.service.model.*;
import org.mapstruct.Mapper;

@Mapper
public interface RestMapper {
    AccountBO toBO(AccountDTO accountDTO);
    AccountDTO toDTO(AccountBO accountBO);

    CredentialsBO toBO(CredentialsDTO credentialsDTO);
    CredentialsDTO toDTO(CredentialsBO credentialsBO);

    TokensBO toBO(TokensDTO tokensDTO);
    TokensDTO toDTO(TokensBO tokensBO);

    PermissionBO toBO(PermissionDTO permissionDTO);
    PermissionDTO toDTO(PermissionBO permissionBO);

    RoleBO toBO(RoleDTO permissionDTO);
    RoleDTO toDTO(RoleBO permissionBO);

    AppBO toBO(AppDTO appDTO);
    AppDTO toDTO(AppBO appBO);
}
