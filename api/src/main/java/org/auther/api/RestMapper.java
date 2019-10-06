package org.auther.api;

import org.auther.api.dto.*;
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

    PermissionGroupBO toBO(PermissionGroupDTO permissionGroupDTO);
    PermissionGroupDTO toDTO(PermissionGroupBO permissionGroupBO);

    PermissionBO toBO(PermissionDTO permissionDTO);
    PermissionDTO toDTO(PermissionBO permissionBO);
}
