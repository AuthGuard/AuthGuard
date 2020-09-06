package com.authguard.api.dto.validation.validators;

import com.authguard.api.dto.entities.PermissionDTO;
import com.authguard.api.dto.entities.TokenRestrictionsDTO;
import com.authguard.api.dto.entities.UserIdentifierDTO;
import com.authguard.api.dto.requests.*;
import com.authguard.api.dto.validation.Validator;

import java.util.HashMap;
import java.util.Map;

public class Validators {
    private Validators() {}

    private static final Map<Class<?>, Validator<?>> classValidators = new HashMap<>();

    static {
        classValidators.put(CreateAccountRequestDTO.class, new CreateAccountRequestValidator());
        classValidators.put(CreateAppRequestDTO.class, new CreateAppRequestValidator());
        classValidators.put(CreateCredentialsRequestDTO.class, new CreateCredentialsRequestValidator());
        classValidators.put(AuthRequestDTO.class, new AuthRequestValidator());
        classValidators.put(PasswordlessRequestDTO.class, new PasswordlessRequestValidator());
        classValidators.put(PermissionsRequestDTO.class, new PermissionsRequestValidator());
        classValidators.put(AccountEmailsRequestDTO.class, new AccountEmailsRequestValidator());

        classValidators.put(TokenRestrictionsDTO.class, new TokenRestrictionsValidator());
        classValidators.put(OtpRequestDTO.class, new OtpRequestValidator());
        classValidators.put(PermissionDTO.class, new PermissionValidator());
        classValidators.put(UserIdentifiersRequestDTO.class, new UserIdentifiersRequestValidator());
        classValidators.put(UserIdentifierDTO.class, new UserIdentifierValidator());
    }

    @SuppressWarnings("unchecked")
    public static <T> Validator<T> getForClass(final Class<T> clazz) {
        return (Validator<T>) classValidators.get(clazz);
    }

}
