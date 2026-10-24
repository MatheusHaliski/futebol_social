package com.footballai.social.config;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Cifra PII em repouso (AES-256-GCM, IV aleatório de 12 bytes prefixado, base64). RNF: PII criptografada.
 * A chave é injetada por {@link CryptoConfig}; em testes/sem configuração usa uma chave de desenvolvimento derivada.
 */
@Converter
public class CryptoConverter implements AttributeConverter<String, String> {
    private static final String ALG = "AES/GCM/NoPadding";
    private static final int IV_LEN = 12, TAG_BITS = 128;
    private static final AtomicReference<SecretKey> KEY = new AtomicReference<>(devKey());
    private static final SecureRandom RNG = new SecureRandom();

    static void setKey(byte[] raw) {
        if (raw.length != 32) throw new IllegalArgumentException("app.crypto.key deve ter 32 bytes (AES-256)");
        KEY.set(new SecretKeySpec(raw, "AES"));
    }

    private static SecretKey devKey() {
        try {
            byte[] k = MessageDigest.getInstance("SHA-256").digest("football-ai-social-dev-key".getBytes(StandardCharsets.UTF_8));
            return new SecretKeySpec(k, "AES");
        } catch (Exception e) { throw new IllegalStateException(e); }
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null) return null;
        try {
            byte[] iv = new byte[IV_LEN]; RNG.nextBytes(iv);
            Cipher c = Cipher.getInstance(ALG);
            c.init(Cipher.ENCRYPT_MODE, KEY.get(), new GCMParameterSpec(TAG_BITS, iv));
            byte[] ct = c.doFinal(attribute.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(ByteBuffer.allocate(IV_LEN + ct.length).put(iv).put(ct).array());
        } catch (Exception e) { throw new IllegalStateException("Falha ao cifrar atributo", e); }
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null) return null;
        try {
            byte[] all = Base64.getDecoder().decode(dbData);
            Cipher c = Cipher.getInstance(ALG);
            c.init(Cipher.DECRYPT_MODE, KEY.get(), new GCMParameterSpec(TAG_BITS, all, 0, IV_LEN));
            return new String(c.doFinal(all, IV_LEN, all.length - IV_LEN), StandardCharsets.UTF_8);
        } catch (Exception e) { throw new IllegalStateException("Falha ao decifrar atributo", e); }
    }
}
