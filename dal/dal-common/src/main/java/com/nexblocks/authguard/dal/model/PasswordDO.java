package com.nexblocks.authguard.dal.model;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class PasswordDO {
    private String password;
    private String salt;
}
