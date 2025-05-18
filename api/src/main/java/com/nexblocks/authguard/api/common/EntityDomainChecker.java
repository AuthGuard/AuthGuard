package com.nexblocks.authguard.api.common;

import com.nexblocks.authguard.api.dto.entities.DomainScoped;
import com.nexblocks.authguard.service.Domains;
import com.nexblocks.authguard.service.exceptions.ServiceAuthorizationException;
import com.nexblocks.authguard.service.exceptions.codes.ErrorCode;
import io.javalin.http.Context;
import io.vertx.ext.web.RoutingContext;

import java.util.Objects;

public class EntityDomainChecker {
    public static <T extends DomainScoped> T checkActorAuthorized(final T entity, final Context actor) {
        String actorDomain = ((DomainScoped) actor).getDomain();

        boolean isAuthorized = Objects.equals(actorDomain, Domains.GLOBAL_RESERVED_DOMAIN) ||
                Objects.equals(actorDomain, entity.getDomain());

        if (isAuthorized) {
            return entity;
        }

        throw new ServiceAuthorizationException(ErrorCode.ENTITY_OUT_OF_SCOPE,
                "Entity is out of the scope of the actor", true);
    }

    public static <T extends DomainScoped> void checkEntityDomainOrFail(final T entity, final Context context) {
        String domain = context.pathParam("domain");

        if (domain == null || domain.isBlank()) {
            return;
        }

        if (Objects.equals(entity.getDomain(), domain)) {
            return;
        }

        throw new ServiceAuthorizationException(ErrorCode.ENTITY_OUT_OF_SCOPE,
                "Request does not match the domain", true);
    }

    public static <T extends DomainScoped> void checkEntityDomainOrFail(final T entity, final RoutingContext context) {
        String domain = context.pathParam("domain");

        if (domain == null || domain.isBlank()) {
            return;
        }

        if (Objects.equals(entity.getDomain(), domain)) {
            return;
        }

        throw new ServiceAuthorizationException(ErrorCode.ENTITY_OUT_OF_SCOPE,
                "Request does not match the domain", true);
    }

    public static <T extends DomainScoped> void checkEntityDomainOrFail(final T entity, final String domain) {
        if (domain == null || domain.isBlank()) {
            return;
        }

        if (Objects.equals(entity.getDomain(), domain)) {
            return;
        }

        throw new ServiceAuthorizationException(ErrorCode.ENTITY_OUT_OF_SCOPE,
                "Request does not match the domain", true);
    }
}
