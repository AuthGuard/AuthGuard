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
@Table(name = "exchange_attempts")
@NamedQuery(
        name = "exchange_attempts.getByEntityId",
        query = "SELECT attempt FROM ExchangeAttemptDO attempt " +
                "WHERE attempt.entityId = :entityId"
)
@NamedQuery(
        name = "exchange_attempts.getByEntityIdFromTimestamp",
        query = "SELECT attempt FROM ExchangeAttemptDO attempt " +
                "WHERE attempt.entityId = :entityId AND attempt.createdAt > :timestamp"
)
@NamedQuery(
        name = "exchange_attempts.getByEntityIdAndExchangeFromTimestamp",
        query = "SELECT attempt FROM ExchangeAttemptDO attempt " +
                "WHERE attempt.entityId = :entityId AND attempt.exchangeFrom = :exchangeFrom " +
                "AND attempt.createdAt > :timestamp"
)
public class ExchangeAttemptDO extends AbstractDO {
    private long entityId;
    private String exchangeFrom;
    private String exchangeTo;
    private boolean successful;
    private String deviceId;
    private String clientId;
    private long trackingSessionId;
    private String externalSessionId;
    private String sourceIp;
    private String userAgent;
}
