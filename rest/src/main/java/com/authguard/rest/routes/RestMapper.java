package com.authguard.rest.routes;

import com.authguard.api.dto.*;
import com.authguard.service.model.*;
import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

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

    RoleBO toBO(RoleDTO roleDTO);
    RoleDTO toDTO(RoleBO roleBO);

    AccountEmailBO toBO(AccountEmailDTO accountEmailDTO);
    AccountEmailDTO toDTO(AccountEmailBO accountEmailBO);

    UserIdentifierBO toBO(UserIdentifierDTO userIdentifierDTO);
    UserIdentifierDTO toDTO(UserIdentifierBO userIdentifierBO);

    @Mapping(target = "parentAccountId", source = "accountId")
    AppBO toBO(AppDTO appDTO);
    @InheritInverseConfiguration
    AppDTO toDTO(AppBO appBO);
}
