package com.authguard.rest.mappers;

import com.authguard.api.dto.entities.*;
import com.authguard.api.dto.requests.*;
import com.authguard.service.model.*;
import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface RestMapper {
    AccountBO toBO(CreateAccountRequestDTO createAccountRequestDTO);
    AccountBO toBO(AccountDTO accountDTO);
    AccountDTO toDTO(AccountBO accountBO);

    CredentialsBO toBO(CreateCredentialsRequestDTO createAccountRequestDTO);
    CredentialsBO toBO(CredentialsDTO credentialsDTO);
    CredentialsDTO toDTO(CredentialsBO credentialsBO);

    TokensBO toBO(TokensDTO tokensDTO);
    TokensDTO toDTO(TokensBO tokensBO);

    TokenRestrictionsBO toBO(TokenRestrictionsDTO tokenRestrictionsDTO);
    TokenRestrictionsDTO toDTO(TokenRestrictionsBO tokenRestrictionsBO);

    PermissionBO toBO(PermissionDTO permissionDTO);
    PermissionDTO toDTO(PermissionBO permissionBO);

    RoleBO toBO(RoleDTO roleDTO);
    RoleBO toBO(CreateRoleRequestDTO createRoleRequestDTO);
    RoleDTO toDTO(RoleBO roleBO);

    AccountEmailBO toBO(AccountEmailDTO accountEmailDTO);
    AccountEmailDTO toDTO(AccountEmailBO accountEmailBO);

    UserIdentifierBO toBO(UserIdentifierDTO userIdentifierDTO);
    UserIdentifierDTO toDTO(UserIdentifierBO userIdentifierBO);

    @Mapping(target = "parentAccountId", source = "accountId")
    AppBO toBO(CreateAppRequestDTO createAppRequestDTO);
    @Mapping(target = "parentAccountId", source = "accountId")
    AppBO toBO(AppDTO appDTO);
    @InheritInverseConfiguration
    AppDTO toDTO(AppBO appBO);
}
