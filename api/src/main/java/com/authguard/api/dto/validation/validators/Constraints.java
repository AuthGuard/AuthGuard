package com.authguard.api.dto.validation.validators;

import com.authguard.api.dto.validation.basic.HasItems;
import com.authguard.api.dto.validation.basic.Required;
import com.authguard.api.dto.validation.basic.ValidEmail;
import com.authguard.api.dto.validation.basic.ValidString;

public class Constraints {
    private Constraints() {}

    public static final ValidString reasonableLength = new ValidString();
    public static final ValidEmail validEmail = new ValidEmail();
    public static final Required required = new Required();
    public static final HasItems hasItems = new HasItems();
}
