package com.footballai.social.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.cassandra.repository.config.EnableCassandraRepositories;

/**
 * Cassandra (feeds segmentados e telemetria de alta vazão). Ligado por {@code app.cassandra.enabled=true}
 * (perfil {@code docker}); no perfil {@code local} a autoconfiguração é excluída e o feed cai para MySQL.
 * A conexão vem de {@code spring.cassandra.*}.
 */
@Configuration
@ConditionalOnProperty(name = "app.cassandra.enabled", havingValue = "true")
@EnableCassandraRepositories(basePackages = "com.footballai.social.repository.cassandra")
public class CassandraConfig { }
