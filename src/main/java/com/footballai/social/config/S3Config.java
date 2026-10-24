package com.footballai.social.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;

/** S3 (MinIO local / AWS): mídias em {@code fas-media} e exportações LGPD em {@code fas-lgpd-exports} (7 d). */
@Configuration
public class S3Config {

    @Bean
    public S3Client s3Client(AppProperties props) {
        var s = props.storage();
        var b = S3Client.builder()
                .region(Region.of(s.region()))
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(s.accessKey(), s.secretKey())))
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(s.pathStyle()).build());
        if (s.endpoint() != null && !s.endpoint().isBlank()) b.endpointOverride(URI.create(s.endpoint()));
        return b.build();
    }

    /** Pré-assinatura com o endpoint que o navegador alcança ({@code app.storage.public-endpoint}, senão {@code endpoint}). */
    @Bean
    public S3Presigner s3Presigner(AppProperties props) {
        var s = props.storage();
        var b = S3Presigner.builder()
                .region(Region.of(s.region()))
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(s.accessKey(), s.secretKey())))
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(s.pathStyle()).build());
        if (!s.browserEndpoint().isBlank()) b.endpointOverride(URI.create(s.browserEndpoint()));
        return b.build();
    }
}
