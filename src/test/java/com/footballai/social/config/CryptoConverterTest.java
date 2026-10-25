package com.footballai.social.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** RNF: PII cifrada em repouso (AES-256-GCM com IV aleatório). */
class CryptoConverterTest {
    @Test
    void roundTripAndNonDeterministicCiphertext() {
        CryptoConverter c = new CryptoConverter();
        String a = c.convertToDatabaseColumn("<PREENCHER_EMAIL>@exemplo.com");
        String b = c.convertToDatabaseColumn("<PREENCHER_EMAIL>@exemplo.com");
        assertThat(a).isNotEqualTo(b).doesNotContain("exemplo");
        assertThat(c.convertToEntityAttribute(a)).isEqualTo("<PREENCHER_EMAIL>@exemplo.com");
        assertThat(c.convertToDatabaseColumn(null)).isNull();
    }
}
