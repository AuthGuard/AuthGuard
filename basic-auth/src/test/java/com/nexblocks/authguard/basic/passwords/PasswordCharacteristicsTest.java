package com.nexblocks.authguard.basic.passwords;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordCharacteristicsTest {
    @Test
    void containsDigits() {
        assertThat(new PasswordCharacteristics("p1").containsDigits()).isTrue();
        assertThat(new PasswordCharacteristics("1").containsDigits()).isTrue();
        assertThat(new PasswordCharacteristics("p178").containsDigits()).isTrue();

        assertThat(new PasswordCharacteristics("p").containsDigits()).isFalse();
        assertThat(new PasswordCharacteristics("Full-Password!").containsDigits()).isFalse();
    }

    @Test
    void containsSmallLetters() {
        assertThat(new PasswordCharacteristics("p1").containsSmallLetters()).isTrue();
        assertThat(new PasswordCharacteristics("p178s").containsSmallLetters()).isTrue();
        assertThat(new PasswordCharacteristics("Full-Password!").containsSmallLetters()).isTrue();

        assertThat(new PasswordCharacteristics("P").containsSmallLetters()).isFalse();
        assertThat(new PasswordCharacteristics("1234").containsSmallLetters()).isFalse();
        assertThat(new PasswordCharacteristics("FULL-PASSWORD!").containsSmallLetters()).isFalse();
    }

    @Test
    void containsCapitalLetters() {
        assertThat(new PasswordCharacteristics("P1").containsCapitalLetters()).isTrue();
        assertThat(new PasswordCharacteristics("P178S").containsCapitalLetters()).isTrue();
        assertThat(new PasswordCharacteristics("Full-Password!").containsCapitalLetters()).isTrue();

        assertThat(new PasswordCharacteristics("p").containsCapitalLetters()).isFalse();
        assertThat(new PasswordCharacteristics("1234").containsCapitalLetters()).isFalse();
        assertThat(new PasswordCharacteristics("full-password!").containsCapitalLetters()).isFalse();
    }

    @Test
    void containsSpecialCharacters() {
        assertThat(new PasswordCharacteristics("P1!").containsSpecialCharacters()).isTrue();
        assertThat(new PasswordCharacteristics("P178S@").containsSpecialCharacters()).isTrue();
        assertThat(new PasswordCharacteristics("Full-Password!~)()").containsSpecialCharacters()).isTrue();

        assertThat(new PasswordCharacteristics("p").containsSpecialCharacters()).isFalse();
        assertThat(new PasswordCharacteristics("1234").containsSpecialCharacters()).isFalse();
        assertThat(new PasswordCharacteristics("fullPassword").containsSpecialCharacters()).isFalse();
    }
}