package com.mycompany.myapp.service.invoice;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class VietnamTaxCodeTest {

    @Test
    void acceptsKnownValidMst() {
        assertThat(VietnamTaxCode.isValid("0100233488")).isTrue();
        assertThat(VietnamTaxCode.isValid("0103179782")).isTrue(); // MST bán X.E
        assertThat(VietnamTaxCode.isValid("0314409058-002")).isTrue();
        assertThat(VietnamTaxCode.isValid("0314409058002")).isTrue();
    }

    @Test
    void rejectsFakeOrBadChecksum() {
        assertThat(VietnamTaxCode.isValid("1234567890")).isFalse();
        assertThat(VietnamTaxCode.isValid("0100233480")).isFalse(); // sai checksum
        assertThat(VietnamTaxCode.isValid("12345")).isFalse();
        assertThat(VietnamTaxCode.isValid("")).isFalse();
        assertThat(VietnamTaxCode.isValid(null)).isFalse();
        assertThat(VietnamTaxCode.isValid("abcdefghij")).isFalse();
    }

    @Test
    void normalizeBranch() {
        assertThat(VietnamTaxCode.normalize("0314409058002")).isEqualTo("0314409058-002");
        assertThat(VietnamTaxCode.normalize("0103179782")).isEqualTo("0103179782");
    }
}
