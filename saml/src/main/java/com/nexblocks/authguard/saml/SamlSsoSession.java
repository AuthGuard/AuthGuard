package com.nexblocks.authguard.saml;

import com.nexblocks.authguard.dal.model.AccountTokenDO;
import com.nexblocks.authguard.service.model.AccountBO;
import org.immutables.value.Value;

@Value.Immutable
@Value.Style(
        get = {"is*", "get*"},
        jdkOnly = true,
        validationMethod = Value.Style.ValidationMethod.NONE,
        unsafeDefaultAndDerived = true
)
public interface SamlSsoSession {
    AccountBO getAccount();
    AccountTokenDO getSession();
}
