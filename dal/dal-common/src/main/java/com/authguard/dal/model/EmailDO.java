package com.authguard.dal.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import javax.persistence.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
// JPA
@Embeddable
public class EmailDO {
    private String email;
    private boolean verified;

    private boolean active;
}
