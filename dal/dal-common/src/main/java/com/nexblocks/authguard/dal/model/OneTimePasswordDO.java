package com.nexblocks.authguard.dal.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import javax.persistence.Entity;
import javax.persistence.Table;
import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
// JPA
@Entity
@Table(name = "otps")
public class OneTimePasswordDO extends AbstractDO {
    private String accountId;
    private String password;
    private Instant expiresAt;
    private String deviceId;
    private String clientId;
    private String externalSessionId;
    private String sourceIp;
    private String userAgent;
}
