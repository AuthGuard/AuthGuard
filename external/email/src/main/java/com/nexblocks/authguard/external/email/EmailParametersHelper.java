package com.nexblocks.authguard.external.email;

import java.util.HashMap;
import java.util.Map;

public class EmailParametersHelper {
    public static Map<String, Object> combineParameters(final Map<String, String> defaultParameters,
                                                  final Map<String, Object> suppliedParameters) {
        final Map<String, Object> combinedParameters = new HashMap<>(suppliedParameters);

        defaultParameters.forEach((key, value) -> {
            if (!combinedParameters.containsKey(key)) {
                combinedParameters.put(key, value);
            }
        });

        return combinedParameters;
    }
}
