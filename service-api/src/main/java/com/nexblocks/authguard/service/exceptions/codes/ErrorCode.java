package com.nexblocks.authguard.service.exceptions.codes;

public enum ErrorCode {
    ACCOUNT_DOES_NOT_EXIST("AC.011"),
    ACCOUNT_DUPLICATE_EMAILS("AC.031"),
    ACCOUNT_DUPLICATE_PHONE_NUMBER("AC.032"),
    ACCOUNT_EMAIL_REQUIRED("AC.032"),
    ACCOUNT_PHONE_NUMBER_REQUIRED("AC.032"),
    ACCOUNT_INACTIVE("AC.033"),

    APP_DOES_NOT_EXIST("AP.011"),

    API_KEY_DOES_NOT_EXIST("AK.011"),
    INVALID_API_KEY_TYPE("AK.031"),

    PERMISSION_DOES_NOT_EXIST("PR.011"),
    PERMISSION_ALREADY_EXIST("PR.012"),

    ROLE_DOES_NOT_EXIST("RL.011"),
    ROLE_ALREADY_EXISTS("RL.012"),

    CREDENTIALS_DOES_NOT_EXIST("CD.011"),
    IDENTIFIER_ALREADY_EXISTS("CD.012"),
    IDENTIFIER_DOES_NOT_EXIST("CD.013"),

    IDEMPOTENCY_ERROR("ID.0.12"),

    EXPIRED_TOKEN("TK.021"),
    INVALID_TOKEN("TK.022"),

    UNKNOWN_EXCHANGE("AT.021"),
    UNSUPPORTED_SCHEME("AT.022"),
    INVALID_AUTHORIZATION_FORMAT("AT.023"),
    INVALID_ADDITIONAL_INFORMATION_TYPE("AT.024"),
    TOKEN_EXPIRED_OR_DOES_NOT_EXIST("AT.025"),
    TOKEN_GENERATION_FAILED("AT.031"),
    ACCOUNT_IS_LOCKED("AT.032"),
    INACTIVE_IDENTIFIER("AT.033"),
    GENERIC_AUTH_FAILURE("AT.039"),

    UNSUPPORTED_JWT_ALGORITHM("JT.021"),
    ENCRYPTION_NOT_SUPPORTED("JT.022"),

    PASSWORDS_DO_NOT_MATCH("PW.021"),
    INVALID_PASSWORD("PW.022"),
    PASSWORD_EXPIRED("PW.023"),

    LDAP_MULTIPLE_PASSWORD_ENTRIES("LD.021"),
    LDAP_ERROR("LD.031"),

    MISSING_REQUEST_QUERY("RQ.011"),
    INVALID_REQUEST_QUERY("RQ.012"),
    INVALID_REQUEST_VALUE("RQ.013"),
    ENTITY_OUT_OF_SCOPE("RQ.0.014");

    private final String code;

    ErrorCode(final String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
