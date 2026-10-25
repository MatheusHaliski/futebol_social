package com.footballai.social;

import com.footballai.social.config.AppProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Football AI · Social — ponto de entrada.
 * Identity · Feed · Rarity System. Dados pessoais de exemplo ficam como {@code <PREENCHER_*>}.
 */
@SpringBootApplication
@EnableScheduling
@EnableJpaAuditing
@EnableConfigurationProperties(AppProperties.class)
public class FootballAiSocialApplication {
    public static void main(String[] args) {
        SpringApplication.run(FootballAiSocialApplication.class, args);
    }
}
