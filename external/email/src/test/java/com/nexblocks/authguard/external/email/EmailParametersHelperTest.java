package com.nexblocks.authguard.external.email;

import com.google.common.collect.ImmutableMap;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Map;

class EmailParametersHelperTest {

    @Test
    void combineParameters() {
        final Map<String, String> defaultParameters = ImmutableMap
                .of("key-1", "value-1", "key-2", "value-2");
        final Map<String, Object> suppliedParameters = ImmutableMap
                .of("key-1", "another-value", "key-3", "value-3");

        final Map<String, Object> expected = ImmutableMap
                .of("key-1", "another-value",
                        "key-3", "value-3",
                        "key-2", "value-2");
        final Map<String, Object> actual = EmailParametersHelper.combineParameters(defaultParameters, suppliedParameters);

        Assertions.assertThat(actual).containsExactlyEntriesOf(expected);
    }
}