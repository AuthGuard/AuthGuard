package com.nexblocks.authguard.service.util;

import com.nexblocks.authguard.service.model.AccountEmailBO;
import com.nexblocks.authguard.service.model.PhoneNumberBO;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ValueComparatorTest {

    @Test
    void emailsEqualSameEmailDifferentFlags() {
        final AccountEmailBO first = AccountEmailBO.builder()
                .email("email")
                .verified(true)
                .build();

        final AccountEmailBO second = AccountEmailBO.builder()
                .email("email")
                .build();

        assertThat(ValueComparator.emailsEqual(first, second)).isTrue();
    }


    @Test
    void emailsEqualDifferenceEmails() {
        final AccountEmailBO first = AccountEmailBO.builder()
                .email("email")
                .build();

        final AccountEmailBO second = AccountEmailBO.builder()
                .email("other")
                .build();

        assertThat(ValueComparator.emailsEqual(first, second)).isFalse();
    }

    @Test
    void emailsEqualNullToValue() {
        final AccountEmailBO first = AccountEmailBO.builder()
                .email("email")
                .verified(true)
                .build();

        assertThat(ValueComparator.emailsEqual(first, null)).isFalse();
    }

    @Test
    void emailsEqualValueToNull() {
        final AccountEmailBO second = AccountEmailBO.builder()
                .email("other")
                .build();

        assertThat(ValueComparator.emailsEqual(null, second)).isFalse();
    }

    @Test
    void phoneNumbersEqualSameNumberDifferentFlags() {
        final PhoneNumberBO first = PhoneNumberBO.builder()
                .number("number")
                .verified(true)
                .build();

        final PhoneNumberBO second = PhoneNumberBO.builder()
                .number("number")
                .build();

        assertThat(ValueComparator.phoneNumbersEqual(first, second)).isTrue();
    }

    @Test
    void phoneNumbersEqualDifferentNumbers() {
        final PhoneNumberBO first = PhoneNumberBO.builder()
                .number("number")
                .build();

        final PhoneNumberBO second = PhoneNumberBO.builder()
                .number("other")
                .build();

        assertThat(ValueComparator.phoneNumbersEqual(first, second)).isFalse();
    }

    @Test
    void phoneNumbersEqualValueToNull() {
        final PhoneNumberBO first = PhoneNumberBO.builder()
                .number("number")
                .build();

        assertThat(ValueComparator.phoneNumbersEqual(first, null)).isFalse();
    }

    @Test
    void phoneNumbersEqualNullToValue() {
        final PhoneNumberBO second = PhoneNumberBO.builder()
                .number("number")
                .build();

        assertThat(ValueComparator.phoneNumbersEqual(null, second)).isFalse();
    }
}