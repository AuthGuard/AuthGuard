package com.nexblocks.authguard.dal.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import jakarta.persistence.Entity;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
// JPA
@Entity
@Table(name = "clients")
@NamedQuery(
        name = "clients.getById",
        query = "SELECT client FROM ClientDO client WHERE client.id = :id AND client.deleted = false"
)
@NamedQuery(
        name = "clients.getByExternalId",
        query = "SELECT client FROM ClientDO client WHERE client.externalId = :externalId AND client.deleted = false"
)
@NamedQuery(
        name = "clients.getByAccountId",
        query = "SELECT client FROM ClientDO client WHERE client.accountId = :parentAccountId " +
                "AND client.deleted = false AND client.id > :cursor " +
                "ORDER BY client.id "
)
@NamedQuery(
        name = "clients.getByClientType",
        query = "SELECT client FROM ClientDO client WHERE client.clientType = :clientType " +
                "AND client.deleted = false AND client.id > :cursor " +
                "ORDER BY client.id "
)
@NamedQuery(
        name = "clients.getByDomain",
        query = "SELECT client FROM ClientDO client WHERE client.domain = :domain " +
                "AND client.deleted = false AND client.id > :cursor " +
                "ORDER BY client.id "
)
public class ClientDO extends AbstractDO {
    private String externalId;
    private String name;
    private Long accountId;
    private String domain;
    private String baseUrl;
    private String clientType;

    private boolean active;
}
