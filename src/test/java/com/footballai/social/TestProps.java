package com.footballai.social;

import com.footballai.social.config.AppProperties;

import java.time.Duration;
import java.util.List;

/** AppProperties para testes unitários (sem Spring). */
public final class TestProps {
    private TestProps() { }
    public static AppProperties props() {
        return new AppProperties(
                new AppProperties.Jwt("unit-test-secret-" + "x".repeat(80), Duration.ofMinutes(15), Duration.ofDays(7), "football-ai-social"),
                new AppProperties.RateLimit(120, 300, 10, 5),
                new AppProperties.Storage("http://localhost:9000", "us-east-1", "a", "b", "fas-media", "fas-lgpd-exports", true),
                new AppProperties.Lgpd(30, 7, 5, "0 30 3 * * *"),
                new AppProperties.Cors(List.of("http://localhost:8080")),
                new AppProperties.Crypto(""),
                new AppProperties.Terms("2026.10"),
                new AppProperties.Cassandra(false),
                new AppProperties.Rarity("0 0 3 * * MON"));
    }
}
