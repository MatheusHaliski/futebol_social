package com.footballai.social.service;

import com.footballai.social.config.AppProperties;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.time.Duration;
import java.util.List;

/** S3/MinIO: mídias e exportações LGPD (URL pré-assinada, máx. 7 dias). */
@Service
public class StorageService {
    private static final Logger log = LoggerFactory.getLogger(StorageService.class);
    private final S3Client s3;
    private final S3Presigner presigner;
    private final AppProperties props;

    public StorageService(S3Client s3, S3Presigner presigner, AppProperties props) { this.s3 = s3; this.presigner = presigner; this.props = props; }

    /** Garante os buckets (mídia e exportações com expiração de 7 dias) — útil fora do docker-compose. Não derruba a app se o S3 estiver fora. */
    @EventListener(ApplicationReadyEvent.class)
    public void ensureBuckets() {
        for (String bucket : List.of(props.storage().mediaBucket(), props.storage().exportBucket())) {
            try {
                boolean exists;
                try { s3.headBucket(HeadBucketRequest.builder().bucket(bucket).build()); exists = true; } catch (NoSuchBucketException e) { exists = false; }
                if (!exists) { s3.createBucket(CreateBucketRequest.builder().bucket(bucket).build()); log.info("S3: bucket {} criado", bucket); }
                if (bucket.equals(props.storage().exportBucket())) {
                    s3.putBucketLifecycleConfiguration(PutBucketLifecycleConfigurationRequest.builder().bucket(bucket).lifecycleConfiguration(BucketLifecycleConfiguration.builder()
                            .rules(LifecycleRule.builder().id("lgpd-export-7d").status(ExpirationStatus.ENABLED).filter(LifecycleRuleFilter.builder().prefix("exports/").build())
                                    .expiration(LifecycleExpiration.builder().days(props.lgpd().exportTtlDays()).build()).build()).build()).build());
                }
            } catch (Exception e) { log.warn("S3 indisponível em {} ({}): exportações/mídia ficarão FAILED até o storage subir", props.storage().endpoint(), e.getClass().getSimpleName()); return; }
        }
        log.info("S3 pronto em {} (buckets {} e {})", props.storage().endpoint(), props.storage().mediaBucket(), props.storage().exportBucket());
    }

    public void putExport(String key, byte[] bytes) {
        s3.putObject(PutObjectRequest.builder().bucket(props.storage().exportBucket()).key(key).contentType("application/json").build(), RequestBody.fromBytes(bytes));
    }

    public String presignExport(String key, Duration ttl) {
        Duration capped = ttl.compareTo(Duration.ofDays(7)) > 0 ? Duration.ofDays(7) : ttl;
        return presigner.presignGetObject(GetObjectPresignRequest.builder().signatureDuration(capped)
                .getObjectRequest(GetObjectRequest.builder().bucket(props.storage().exportBucket()).key(key).build()).build()).url().toString();
    }

    public void deleteExport(String key) {
        s3.deleteObject(DeleteObjectRequest.builder().bucket(props.storage().exportBucket()).key(key).build());
    }

    public void deleteMedia(String key) {
        s3.deleteObject(DeleteObjectRequest.builder().bucket(props.storage().mediaBucket()).key(key).build());
    }

    /** Upload de fotografia do card (bucket de mídia). Devolve a chave; a URL é pré-assinada na leitura. */
    public String putMedia(String key, byte[] bytes, String contentType) {
        s3.putObject(PutObjectRequest.builder().bucket(props.storage().mediaBucket()).key(key).contentType(contentType).build(), RequestBody.fromBytes(bytes));
        return key;
    }

    /** URL pré-assinada de leitura da mídia (1 h). Sem storage, devolve a URL direta (demo). */
    public String mediaUrl(String key) {
        try {
            return presigner.presignGetObject(GetObjectPresignRequest.builder().signatureDuration(Duration.ofHours(1))
                    .getObjectRequest(GetObjectRequest.builder().bucket(props.storage().mediaBucket()).key(key).build()).build()).url().toString();
        } catch (RuntimeException e) {
            return props.storage().browserEndpoint() + "/" + props.storage().mediaBucket() + "/" + key;
        }
    }
}
