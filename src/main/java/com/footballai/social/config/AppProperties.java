package com.footballai.social.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;
import java.util.List;

/** Propriedades da aplicação (prefixo {@code app}). Segredos vêm de variáveis de ambiente — nunca do repositório. */
@ConfigurationProperties(prefix = "app")
public record AppProperties(
        @DefaultValue Jwt jwt,
        @DefaultValue RateLimit rateLimit,
        @DefaultValue Storage storage,
        @DefaultValue Lgpd lgpd,
        @DefaultValue Cors cors,
        @DefaultValue Crypto crypto,
        @DefaultValue Terms terms,
        @DefaultValue Cassandra cassandra,
        @DefaultValue Rarity rarity) {

    public record Jwt(@DefaultValue("change-me-in-env-<PREENCHER_JWT_SECRET>") String secret,
                      @DefaultValue("15m") Duration accessTtl,
                      @DefaultValue("7d") Duration refreshTtl,
                      @DefaultValue("football-ai-social") String issuer) { }

    public record RateLimit(@DefaultValue("120") int ipPerMinute,
                            @DefaultValue("300") int userPerMinute,
                            @DefaultValue("10") int loginPerMinute,
                            @DefaultValue("5") int maxLoginFailures) { }

    public record Storage(@DefaultValue("http://localhost:9000") String endpoint,
                          @DefaultValue("us-east-1") String region,
                          @DefaultValue("minioadmin") String accessKey,
                          @DefaultValue("minioadmin") String secretKey,
                          @DefaultValue("fas-media") String mediaBucket,
                          @DefaultValue("fas-lgpd-exports") String exportBucket,
                          @DefaultValue("true") boolean pathStyle) { }

    public record Lgpd(@DefaultValue("30") int deletionGraceDays,
                       @DefaultValue("7") int exportTtlDays,
                       @DefaultValue("5") int consentRetentionYears,
                       @DefaultValue("0 30 3 * * *") String anonymizationCron) { }

    public record Cors(@DefaultValue({"http://localhost:8080", "http://localhost:5173"}) List<String> allowedOrigins) { }

    /** Chave AES-256 em base64 (32 bytes). Sem valor, usa chave de desenvolvimento derivada (apenas local). */
    public record Crypto(@DefaultValue("") String key) { }

    public record Terms(@DefaultValue("2026.10") String currentVersion) { }

    public record Cassandra(@DefaultValue("false") boolean enabled) { }

    public record Rarity(@DefaultValue("0 0 3 * * MON") String cron) { }
}
