package com.authguard.dal.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class EmailDO {
    private String email;
    private boolean verified;
    private boolean primary;
    private boolean backup;
    private boolean active;
}
