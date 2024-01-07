package com.nexblocks.authguard.jwt.oauth.service;

public class OAuthConst {
    public static class Params {

        public static final String ResponseType = "response_type";
        public static final String RedirectUri = "redirect_uri";
        public static final String State = "state";
        public static final String Scope = "scope";
        public static final String CodeChallengeMethod = "code_challenge_method";
        public static final String CodeChallenge = "code_challenge";
        public static final String CodeVerifier = "code_verifier";
        public static final String Token = "token";
        public static final String GrantType = "grant_type";
        public static final String ClientId = "client_id";
        public static final String ClientSecret = "client_secret";
    }

    public static class ResponseTypes {
        public static final String Code = "code";
    }

    public static class GrantTypes {
        public static final String AuthorizationCode = "authorization_code";
        public static final String RefreshToken = "refresh_token";
    }

    public static class ErrorsMessages {
        public static final String UnsupportedResponseType = "unsupported_response_type";
        public static final String UnauthorizedClient = "unauthorized_client";
        public static final String UnknownError = "unknown_error";
        public static final String InvalidGrant = "invalid_grant";
        public static final String InvalidRequest = "invalid_request";
    }
}
