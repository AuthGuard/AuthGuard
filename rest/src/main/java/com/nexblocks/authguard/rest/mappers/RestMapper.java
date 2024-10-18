package com.nexblocks.authguard.rest.mappers;

import com.nexblocks.authguard.api.dto.entities.*;
import com.nexblocks.authguard.api.dto.requests.*;
import com.nexblocks.authguard.service.model.*;
import org.apache.commons.codec.binary.Base32;
import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface RestMapper {
    AccountBO toBO(CreateAccountRequestDTO createAccountRequestDTO);
    AccountBO toBO(UpdateAccountRequestDTO updateAccountRequestDTO);
    AccountBO toBO(AccountDTO accountDTO);
    AccountDTO toDTO(AccountBO accountBO);

    CredentialsBO toBO(CredentialsDTO credentialsDTO);
    CredentialsDTO toDTO(CredentialsBO credentialsBO);

    AuthRequestBO toBO(AuthRequestDTO authRequestDTO);
    AuthRequestDTO toDO(AuthRequestBO authRequestBO);

    AuthResponseBO toBO(AuthResponseDTO tokensDTO);
    AuthResponseDTO toDTO(AuthResponseBO tokensBO);

    TokenRestrictionsBO toBO(TokenRestrictionsDTO tokenRestrictionsDTO);
    TokenRestrictionsDTO toDTO(TokenRestrictionsBO tokenRestrictionsBO);

    PermissionBO toBO(PermissionDTO permissionDTO);
    PermissionBO toBO(CreatePermissionRequestDTO createPermissionRequestDTO);
    PermissionBO toBO(UpdatePermissionRequestDTO updatePermissionRequestDTO);
    PermissionDTO toDTO(PermissionBO permissionBO);

    RoleBO toBO(RoleDTO roleDTO);
    RoleBO toBO(CreateRoleRequestDTO createRoleRequestDTO);
    RoleBO toBO(UpdateRoleRequestDTO updateRoleRequestDTO);
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

    ClientBO toBO(CreateClientRequestDTO createClientRequestDTO);
    ClientBO toBO(ClientDTO clientDTO);
    @InheritInverseConfiguration
    ClientDTO toDTO(ClientBO clientBO);

    ApiKeyBO toBO(ApiKeyDTO apiKeyDO);
    ApiKeyDTO toDTO(ApiKeyBO apiKeyBO);

    AccountLockBO toBO(AccountLockDTO accountLockDTO);
    AccountLockDTO toDTO(AccountLockBO accountLockBO);

    ExchangeAttemptDTO toDTO(ExchangeAttemptBO exchangeAttemptBO);

    PasswordResetTokenDTO toDTO(PasswordResetTokenBO passwordResetTokenBO);

    ActionTokenDTO toDTO(ActionTokenBO actionTokenBO);

    EventDTO toDTO(EventBO eventBO);

    CryptoKeyDTO toDTO(EphemeralKeyBO ephemeralKeyBO);
    CryptoKeyDTO toDTO(PersistedKeyBO persistedKeyBO);

    TotpKeyDTO toDTO(TotpKeyBO totpKeyBO);

    default String toBase32(final byte[] blob) {
        return blob == null ? null : InternalBase32.encode(blob);
    }

    class InternalBase32 {
        private static final Base32 base32 = new Base32();

        public static String encode(final byte[] bytes) {
            return base32.encodeToString(bytes);
        }
    }
}
