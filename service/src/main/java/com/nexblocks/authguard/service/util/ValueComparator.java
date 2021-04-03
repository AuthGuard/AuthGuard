package com.nexblocks.authguard.service.util;

import com.nexblocks.authguard.service.model.AccountEmailBO;
import com.nexblocks.authguard.service.model.PhoneNumberBO;

import java.util.Objects;

/**
 * Contains util functions to compare objects based only
 * a certain field. Everything here must be null safe.
 */
public class ValueComparator {
    public static boolean emailsEqual(final AccountEmailBO first, final AccountEmailBO second) {
        final String firstValue = first == null ? null : first.getEmail();
        final String secondValue = second == null ? null : second.getEmail();

        return Objects.equals(firstValue, secondValue);
    }

    public static boolean phoneNumbersEqual(final PhoneNumberBO first, final PhoneNumberBO second) {
        final String firstValue = first == null ? null : first.getNumber();
        final String secondValue = second == null ? null : second.getNumber();

        return Objects.equals(firstValue, secondValue);
    }
}
