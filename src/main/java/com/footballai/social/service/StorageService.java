package com.footballai.social.service;

import com.footballai.social.config.AppProperties;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.time.Duration;

/** S3/MinIO: mídias e exportações LGPD (URL pré-assinada, máx. 7 dias). */
@Service
public class StorageService {
    private final S3Client s3;
    private final S3Presigner presigner;
    private final AppProperties props;

    public StorageService(S3Client s3, S3Presigner presigner, AppProperties props) { this.s3 = s3; this.presigner = presigner; this.props = props; }

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

    public String mediaUrl(String key) {
        return props.storage().endpoint() + "/" + props.storage().mediaBucket() + "/" + key;
    }
}
