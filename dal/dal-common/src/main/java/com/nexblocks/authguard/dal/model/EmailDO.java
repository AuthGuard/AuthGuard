package com.nexblocks.authguard.dal.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Embeddable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
// JPA
@Embeddable
public class EmailDO {
    private String email;
    private boolean verified;
}
