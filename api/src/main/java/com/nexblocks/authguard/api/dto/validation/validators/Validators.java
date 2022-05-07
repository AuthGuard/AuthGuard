package com.nexblocks.authguard.api.dto.validation.validators;

import com.nexblocks.authguard.api.dto.entities.PermissionDTO;
import com.nexblocks.authguard.api.dto.entities.TokenRestrictionsDTO;
import com.nexblocks.authguard.api.dto.entities.UserIdentifierDTO;
import com.nexblocks.authguard.api.dto.requests.*;
import com.nexblocks.authguard.api.dto.validation.Validator;

import java.util.HashMap;
import java.util.Map;

public class Validators {
    private Validators() {}

    private static final Map<Class<?>, Validator<?>> classValidators = new HashMap<>();

    static {
        classValidators.put(CreateAccountRequestDTO.class, new CreateAccountRequestValidator());
        classValidators.put(UpdateAccountRequestDTO.class, new UpdateAccountRequestValidator());
        classValidators.put(CreateAppRequestDTO.class, new CreateAppRequestValidator());
        classValidators.put(CreateCredentialsRequestDTO.class, new CreateCredentialsRequestValidator());
        classValidators.put(CreateCompleteAccountRequestDTO.class, new CreateCompleteAccountRequestValidator());
        classValidators.put(AuthRequestDTO.class, new AuthRequestValidator());
        classValidators.put(PasswordlessRequestDTO.class, new PasswordlessRequestValidator());
        classValidators.put(PermissionsRequestDTO.class, new PermissionsRequestValidator());
        classValidators.put(RolesRequestDTO.class, new RolesRequestValidator());
        classValidators.put(CreateRoleRequestDTO.class, new CreateRoleRequestValidator());
        classValidators.put(CreatePermissionRequestDTO.class, new CreatePermissionRequestValidator());
        classValidators.put(AccountEmailsRequestDTO.class, new AccountEmailsRequestValidator());

        classValidators.put(TokenRestrictionsDTO.class, new TokenRestrictionsValidator());
        classValidators.put(OtpRequestDTO.class, new OtpRequestValidator());
        classValidators.put(PermissionDTO.class, new PermissionValidator());
        classValidators.put(UserIdentifiersRequestDTO.class, new UserIdentifiersRequestValidator());
        classValidators.put(UserIdentifierDTO.class, new UserIdentifierValidator());
        classValidators.put(ApiKeyRequestDTO.class, new ApiKeysRequestValidator());
        classValidators.put(ApiKeyVerificationRequestDTO.class, new ApiKeyVerificationRequestValidator());
        classValidators.put(ActionTokenRequestDTO.class, new ActionTokensRequestValidator());

        classValidators.put(PasswordResetTokenRequestDTO.class, new PasswordResetTokenRequestValidator());
        classValidators.put(PasswordResetRequestDTO.class, new PasswordResetRequestValidator());
    }

    @SuppressWarnings("unchecked")
    public static <T> Validator<T> getForClass(final Class<T> clazz) {
        return (Validator<T>) classValidators.get(clazz);
    }

}
