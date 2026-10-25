package com.footballai.social.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

import java.util.Base64;

/** Injeta a chave AES-256 (base64, 32 bytes) de {@code app.crypto.key} no conversor de PII. */
@Configuration
public class CryptoConfig {
    private static final Logger log = LoggerFactory.getLogger(CryptoConfig.class);

    public CryptoConfig(AppProperties props) {
        String key = props.crypto().key();
        if (key == null || key.isBlank()) {
            log.warn("app.crypto.key não configurada — usando chave de DESENVOLVIMENTO. Defina APP_CRYPTO_KEY (base64 de 32 bytes) em produção.");
            return;
        }
        CryptoConverter.setKey(Base64.getDecoder().decode(key));
        log.info("Chave AES-256 de PII carregada do ambiente.");
    }
}
